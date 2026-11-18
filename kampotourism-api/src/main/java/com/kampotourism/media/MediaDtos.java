package com.kampotourism.media;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class MediaDtos {

    private MediaDtos() {
    }

    // ---- public ----

    public record ImageDto(String url, String alt, String caption, Integer width, Integer height) {
    }

    public record OwnerImages(ImageDto cover, List<ImageDto> gallery) {
    }

    // ---- admin ----

    public record AdminImageRef(Long mediaId, String url, String altText, String caption) {
    }

    public record AdminImages(AdminImageRef cover, List<AdminImageRef> gallery) {
    }

    public record MediaTextBody(@Size(max = 300) String altText, @Size(max = 500) String caption) {
    }

    /** Replaces all alt text / captions of an asset. Keys are language codes. */
    public record UpdateMediaTextsRequest(
            @NotNull @Size(min = 1, max = 20) Map<String, @Valid MediaTextBody> translations) {
    }

    public record UsageDto(String ownerType, Long ownerId, String role) {
    }

    public record AdminMediaView(Long id, long version, String url, String mimeType, long sizeBytes,
                                 Integer width, Integer height, String originalFilename,
                                 Instant updatedAt, Map<String, MediaTextBody> translations,
                                 List<UsageDto> usages) {
    }

    public record AdminMediaSummary(Long id, String url, String originalFilename, String mimeType,
                                    long sizeBytes, Integer width, Integer height, String altText,
                                    long usageCount, Instant updatedAt) {
    }
}
