package com.kampotourism.media;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kampotourism.config.MediaProperties;
import com.sun.net.httpserver.HttpServer;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

/** Cloudinary storage against a local fake Upload API: no network, no real account. */
class CloudinaryMediaStorageTest {

    private static final String SECRET = "test-api-secret";
    private static final Pattern FIELD = Pattern.compile("name=\"(\\w+)\"\r\n\r\n([^\r]*)\r\n");

    @TempDir
    Path scratch;

    private HttpServer server;
    private final List<Map<String, String>> uploads = new CopyOnWriteArrayList<>();
    private final List<Map<String, String>> destroys = new CopyOnWriteArrayList<>();
    private volatile int uploadStatus = 200;

    @BeforeEach
    void startFakeCloudinary() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1_1/demo/image/upload", exchange -> {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.ISO_8859_1);
            Map<String, String> fields = new HashMap<>();
            Matcher m = FIELD.matcher(body);
            while (m.find()) {
                fields.put(m.group(1), m.group(2));
            }
            fields.put("_hasFile", Boolean.toString(body.contains("name=\"file\"") && body.contains("PNG")));
            uploads.add(fields);
            String response = uploadStatus == 200
                    ? "{\"public_id\":\"" + fields.get("public_id") + "\"}"
                    : "{\"error\":{\"message\":\"Invalid Signature\"}}";
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(uploadStatus, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.createContext("/v1_1/demo/image/destroy", exchange -> {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            Map<String, String> fields = new HashMap<>();
            for (String pair : body.split("&")) {
                String[] kv = pair.split("=", 2);
                fields.put(kv[0], URLDecoder.decode(kv[1], StandardCharsets.UTF_8));
            }
            destroys.add(fields);
            byte[] bytes = "{\"result\":\"ok\"}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    private MediaProperties properties() {
        return new MediaProperties("cloudinary", scratch.toString(), null,
                new MediaProperties.Cloudinary("demo", "123456", SECRET, "kampotourism"));
    }

    private MediaStorage storage(MediaProperties properties) {
        URI api = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/v1_1/");
        return new MediaStorage(properties, new CloudinaryClient(properties.cloudinary(), api));
    }

    private static MockMultipartFile png() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(3, 2, BufferedImage.TYPE_INT_RGB), "png", out);
        return new MockMultipartFile("file", "beach.png", "image/png", out.toByteArray());
    }

    @Test
    void signatureMatchesCloudinaryDocumentedExample() {
        Map<String, String> params = Map.of(
                "eager", "w_400,h_300,c_pad|w_260,h_200,c_crop",
                "public_id", "sample_image",
                "timestamp", "1315060510");
        assertThat(CloudinaryClient.sign(params, "abcd")).isEqualTo("bfd09f95f331f558cbd1320e67aa8d488770583e");
    }

    @Test
    void uploadsSignedFileAndKeepsNoLocalCopy() throws Exception {
        MediaProperties properties = properties();
        MediaStorage storage = storage(properties);

        MediaStorage.StoredFile stored = storage.store(png());

        assertThat(stored.key()).matches("\\d{4}/\\d{2}/[0-9a-f-]{36}\\.png");
        assertThat(stored.width()).isEqualTo(3);
        assertThat(stored.height()).isEqualTo(2);
        assertThat(Files.exists(scratch.resolve(stored.key()))).isFalse();
        assertThat(storage.servesLocalFiles()).isFalse();

        assertThat(uploads).hasSize(1);
        Map<String, String> sent = uploads.get(0);
        String publicId = "kampotourism/" + stored.key().replace(".png", "");
        assertThat(sent).containsEntry("public_id", publicId)
                .containsEntry("api_key", "123456")
                .containsEntry("overwrite", "false")
                .containsEntry("_hasFile", "true")
                .doesNotContainValue(SECRET);
        assertThat(sent.get("signature")).isEqualTo(CloudinaryClient.sign(Map.of(
                "overwrite", "false", "public_id", publicId, "timestamp", sent.get("timestamp")), SECRET));

        assertThat(new MediaUrls(properties).url(stored.key()))
                .isEqualTo("https://res.cloudinary.com/demo/image/upload/kampotourism/" + stored.key());

        storage.delete(stored.key());
        assertThat(destroys).hasSize(1);
        assertThat(destroys.get(0)).containsEntry("public_id", publicId).containsEntry("invalidate", "true");
    }

    @Test
    void failedUploadIsRejectedAndLeavesNoScratchFile() throws Exception {
        uploadStatus = 401;
        MediaStorage storage = storage(properties());

        assertThatThrownBy(() -> storage.store(png()))
                .hasMessageContaining("HTTP 401")
                .hasMessageNotContaining(SECRET);
        try (var files = Files.walk(scratch)) {
            assertThat(files.filter(Files::isRegularFile)).isEmpty();
        }
    }

    @Test
    void cloudinaryModeRequiresCredentials() {
        assertThatThrownBy(() -> new MediaProperties("cloudinary", null, null,
                new MediaProperties.Cloudinary("demo", null, null, null)))
                .hasMessageContaining("CLOUDINARY_API_KEY");
    }
}
