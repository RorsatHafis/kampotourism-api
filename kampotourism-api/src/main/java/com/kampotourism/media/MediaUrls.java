package com.kampotourism.media;

import com.kampotourism.config.MediaProperties;
import org.springframework.stereotype.Component;

/**
 * The database stores only the storage key ("2026/11/uuid.jpg"); the public URL is built
 * on read, so moving to a CDN or another domain is a config change, not a data migration.
 * <ul>
 *   <li>local: {@code <public-base-url>/media/2026/11/uuid.jpg}</li>
 *   <li>cloudinary: {@code https://res.cloudinary.com/<cloud>/image/upload/<folder>/2026/11/uuid.jpg}</li>
 * </ul>
 */
@Component
public class MediaUrls {

    private final String baseUrl;

    public MediaUrls(MediaProperties properties) {
        this.baseUrl = properties.usesCloudinary()
                ? "https://res.cloudinary.com/" + properties.cloudinary().cloudName() + "/image/upload/"
                        + properties.cloudinary().folder()
                : stripTrailingSlash(properties.publicBaseUrl()) + "/media";
    }

    private static String stripTrailingSlash(String base) {
        return base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
    }

    public String url(String storageKey) {
        return baseUrl + "/" + storageKey;
    }
}
