package com.kampotourism.config;

import java.util.Locale;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Where uploaded images live.
 * <ul>
 *   <li>{@code local}: files under {@code dir}, served by this app at {@code /media/**}.
 *       Fine for development; NOT durable on hosts with an ephemeral disk (e.g. Render Free).</li>
 *   <li>{@code cloudinary}: files are pushed to Cloudinary and served from its CDN;
 *       {@code dir} is only a scratch area while an upload is validated.</li>
 * </ul>
 */
@ConfigurationProperties(prefix = "app.media")
public record MediaProperties(String storage, String dir, String publicBaseUrl, Cloudinary cloudinary) {

    public static final String LOCAL = "local";
    public static final String CLOUDINARY = "cloudinary";

    public record Cloudinary(String cloudName, String apiKey, String apiSecret, String folder) {

        public Cloudinary {
            if (folder == null || folder.isBlank()) {
                folder = "kampotourism";
            }
            folder = folder.replaceAll("^/+|/+$", "");
        }
    }

    public MediaProperties {
        storage = (storage == null || storage.isBlank()) ? LOCAL : storage.trim().toLowerCase(Locale.ROOT);
        if (!LOCAL.equals(storage) && !CLOUDINARY.equals(storage)) {
            throw new IllegalStateException("app.media.storage (env MEDIA_STORAGE) must be 'local' or 'cloudinary', got: " + storage);
        }
        if (dir == null || dir.isBlank()) {
            dir = "./uploads";
        }
        if (publicBaseUrl == null || publicBaseUrl.isBlank()) {
            publicBaseUrl = "http://localhost:8080";
        }
        if (cloudinary == null) {
            cloudinary = new Cloudinary(null, null, null, null);
        }
        if (CLOUDINARY.equals(storage) && (isBlank(cloudinary.cloudName()) || isBlank(cloudinary.apiKey())
                || isBlank(cloudinary.apiSecret()))) {
            throw new IllegalStateException("MEDIA_STORAGE=cloudinary requires CLOUDINARY_CLOUD_NAME, "
                    + "CLOUDINARY_API_KEY and CLOUDINARY_API_SECRET.");
        }
    }

    public boolean usesCloudinary() {
        return CLOUDINARY.equals(storage);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
