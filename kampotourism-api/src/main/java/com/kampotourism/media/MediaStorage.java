package com.kampotourism.media;

import com.kampotourism.common.error.BadRequestException;
import com.kampotourism.config.MediaProperties;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Iterator;
import java.util.UUID;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Validates uploads on local disk, then keeps them either on that disk ({@code local}) or in
 * Cloudinary ({@code cloudinary}, see {@link MediaProperties}). Everything else only deals with
 * the returned storage key, e.g. {@code 2026/11/uuid.jpg}.
 */
@Service
public class MediaStorage {

    private static final Logger log = LoggerFactory.getLogger(MediaStorage.class);

    public record StoredFile(String key, String mimeType, long sizeBytes, Integer width, Integer height,
                             String originalFilename) {
    }

    private final Path root;
    private final CloudinaryClient cloudinary;
    private final String cloudinaryFolder;
    private static final int MAX_DIMENSION = 12_000;
    private static final long MAX_PIXELS = 40_000_000L;

    @Autowired
    public MediaStorage(MediaProperties properties) {
        this(properties, properties.usesCloudinary() ? new CloudinaryClient(properties.cloudinary()) : null);
    }

    MediaStorage(MediaProperties properties, CloudinaryClient cloudinary) {
        this.root = Path.of(properties.dir()).toAbsolutePath().normalize();
        this.cloudinary = cloudinary;
        this.cloudinaryFolder = properties.cloudinary().folder();
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot create media directory " + root, e);
        }
        if (cloudinary != null) {
            log.info("Media storage: Cloudinary (folder '{}')", cloudinaryFolder);
        } else {
            log.warn("Media storage: local disk at {}. Files are lost if this disk is not persistent "
                    + "(e.g. Render Free). Use MEDIA_STORAGE=cloudinary in production.", root);
        }
    }

    public Path root() {
        return root;
    }

    /** True when files are kept on local disk and must be served by this app at /media/**. */
    public boolean servesLocalFiles() {
        return cloudinary == null;
    }

    /** Cloudinary public id for a storage key: folder + key without its extension. */
    String cloudinaryPublicId(String key) {
        int dot = key.lastIndexOf('.');
        String withoutExtension = dot > key.lastIndexOf('/') ? key.substring(0, dot) : key;
        return cloudinaryFolder + "/" + withoutExtension;
    }

    /** Accepts JPEG, PNG and WebP only, judged by file signature, never by client-supplied metadata. */
    public StoredFile store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("No file was uploaded.");
        }
        Path target = null;
        try {
            String extension = detectExtension(file.getInputStream().readNBytes(12));
            LocalDate today = LocalDate.now();
            String folder = "%d/%02d".formatted(today.getYear(), today.getMonthValue());
            Files.createDirectories(root.resolve(folder));
            String key = folder + "/" + UUID.randomUUID() + "." + extension;
            target = root.resolve(key);
            file.transferTo(target);
            int[] size = readDimensions(target, extension);
            validateDimensions(size);
            if (cloudinary != null) {
                cloudinary.upload(target, cloudinaryPublicId(key));
                try {
                    Files.deleteIfExists(target);   // local copy was only a scratch file
                } catch (IOException e) {
                    log.warn("Could not remove scratch upload {}", target);
                }
            }
            return new StoredFile(key, mimeFor(extension), file.getSize(), size[0], size[1],
                    cleanFilename(file.getOriginalFilename()));
        } catch (IOException e) {
            throw new UncheckedIOException("Could not store upload", e);
        } catch (RuntimeException e) {
            if (target != null) {
                try {
                    Files.deleteIfExists(target);
                } catch (IOException ignored) {
                    log.warn("Could not remove rejected upload {}", target);
                }
            }
            throw e;
        }
    }

    /** Best-effort delete; a leftover file is harmless, a failed request is not. */
    public void delete(String key) {
        if (cloudinary != null) {
            try {
                cloudinary.destroy(cloudinaryPublicId(key));
            } catch (RuntimeException e) {
                log.warn("Could not delete Cloudinary media {}: {}", key, e.getMessage());
            }
            return;
        }
        try {
            Path target = root.resolve(key).normalize();
            if (target.startsWith(root)) {
                Files.deleteIfExists(target);
            }
        } catch (IOException | RuntimeException e) {
            log.warn("Could not delete media file {}", key, e);
        }
    }

    private static int[] readDimensions(Path path, String extension) {
        File file = path.toFile();
        try (ImageInputStream in = ImageIO.createImageInputStream(file)) {
            if (in == null) {
                return webpDimensions(path, extension);
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
            if (!readers.hasNext()) {
                return webpDimensions(path, extension);
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(in);
                int[] dimensions = new int[] {reader.getWidth(0), reader.getHeight(0)};
                validateDimensions(dimensions);
                // Reading the full image verifies the stream is actually decodable, not merely a valid header.
                reader.read(0);
                return dimensions;
            } finally {
                reader.dispose();
            }
        } catch (IOException | RuntimeException e) {
            return webpDimensions(path, extension);
        }
    }

    /** WebP is not decoded by the stock JDK; validate its container and canvas dimensions directly. */
    private static int[] webpDimensions(Path path, String extension) {
        if (!"webp".equals(extension)) {
            throw new BadRequestException("The uploaded file is not a valid image.");
        }
        try {
            byte[] bytes = Files.readAllBytes(path);
            if (bytes.length < 30 || bytes[0] != 'R' || bytes[1] != 'I' || bytes[2] != 'F' || bytes[3] != 'F'
                    || bytes[8] != 'W' || bytes[9] != 'E' || bytes[10] != 'B' || bytes[11] != 'P') {
                throw new BadRequestException("The uploaded file is not a valid WebP image.");
            }
            String chunk = new String(bytes, 12, 4, java.nio.charset.StandardCharsets.US_ASCII);
            if ("VP8X".equals(chunk) && bytes.length >= 30) {
                return new int[] {onePlus24(bytes, 24), onePlus24(bytes, 27)};
            }
            if ("VP8L".equals(chunk) && bytes.length >= 25 && (bytes[20] & 0xFF) == 0x2F) {
                int bits = (bytes[21] & 0xFF) | ((bytes[22] & 0xFF) << 8) | ((bytes[23] & 0xFF) << 16) | ((bytes[24] & 0xFF) << 24);
                return new int[] {(bits & 0x3FFF) + 1, ((bits >>> 14) & 0x3FFF) + 1};
            }
            if ("VP8 ".equals(chunk) && bytes.length >= 30 && (bytes[23] & 0xFF) == 0x9D
                    && (bytes[24] & 0xFF) == 0x01 && (bytes[25] & 0xFF) == 0x2A) {
                return new int[] {((bytes[26] & 0xFF) | ((bytes[27] & 0x3F) << 8)), ((bytes[28] & 0xFF) | ((bytes[29] & 0x3F) << 8))};
            }
            throw new BadRequestException("The uploaded file is not a supported WebP image.");
        } catch (IOException e) {
            throw new UncheckedIOException("Could not inspect WebP upload", e);
        }
    }

    private static int onePlus24(byte[] data, int offset) {
        return 1 + (data[offset] & 0xFF) + ((data[offset + 1] & 0xFF) << 8) + ((data[offset + 2] & 0xFF) << 16);
    }

    private static void validateDimensions(int[] dimensions) {
        if (dimensions == null || dimensions.length != 2 || dimensions[0] < 1 || dimensions[1] < 1
                || dimensions[0] > MAX_DIMENSION || dimensions[1] > MAX_DIMENSION
                || (long) dimensions[0] * dimensions[1] > MAX_PIXELS) {
            throw new BadRequestException("Image dimensions are invalid or exceed the 40 megapixel limit.");
        }
    }

    private static String cleanFilename(String name) {
        if (name == null) {
            return null;
        }
        String cleaned = name.replace('\\', '/');
        cleaned = cleaned.substring(cleaned.lastIndexOf('/') + 1);
        return cleaned.length() > 255 ? cleaned.substring(0, 255) : cleaned;
    }

    private static String mimeFor(String extension) {
        return switch (extension) {
            case "jpg" -> "image/jpeg";
            case "png" -> "image/png";
            default -> "image/webp";
        };
    }

    private static String detectExtension(byte[] head) {
        if (head.length >= 3 && (head[0] & 0xFF) == 0xFF && (head[1] & 0xFF) == 0xD8 && (head[2] & 0xFF) == 0xFF) {
            return "jpg";
        }
        if (head.length >= 8 && (head[0] & 0xFF) == 0x89 && head[1] == 'P' && head[2] == 'N' && head[3] == 'G'
                && head[4] == 0x0D && head[5] == 0x0A && head[6] == 0x1A && head[7] == 0x0A) {
            return "png";
        }
        if (head.length >= 12 && head[0] == 'R' && head[1] == 'I' && head[2] == 'F' && head[3] == 'F'
                && head[8] == 'W' && head[9] == 'E' && head[10] == 'B' && head[11] == 'P') {
            return "webp";
        }
        throw new BadRequestException("Only JPEG, PNG or WebP images are accepted.");
    }
}
