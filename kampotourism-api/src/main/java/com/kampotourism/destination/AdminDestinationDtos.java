package com.kampotourism.destination;

import com.kampotourism.common.Slugs;
import com.kampotourism.media.MediaDtos.AdminImages;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Admin API shapes: what editors send and what the admin UI reads back. */
public final class AdminDestinationDtos {

    private AdminDestinationDtos() {
    }

    public record HighlightBody(@NotBlank @Size(max = 200) String title, @NotBlank @Size(max = 500) String text) {
    }

    /** All visitor-facing text for one language. Plain text only; the frontend must never render it as HTML. */
    public record TranslationBody(
            @NotBlank @Size(max = 200) String name,
            @Size(max = 200) String tag,
            @NotBlank @Size(max = 500) String summary,
            @NotBlank @Size(max = 5000) String lead,
            @NotNull @Size(max = 20) List<@NotBlank @Size(max = 3000) String> description,
            @NotNull @Size(max = 10) List<@Valid HighlightBody> highlights,
            @NotNull @Size(max = 20) List<@NotBlank @Size(max = 300) String> tips,
            @Size(max = 120) String distance,
            @Size(max = 120) String duration,
            @Size(max = 200) String bestFor,
            @Size(max = 200) String bring,
            @Size(max = 200) String openingHours,
            @Size(max = 120) String bestSeason,
            @Size(max = 300) String entranceFeeNote) {
    }

    public record CreateDestinationRequest(
            @NotBlank @Size(max = 120) @Pattern(regexp = Slugs.PATTERN, message = "must be lower-case words joined by hyphens") String slug,
            @NotNull Long categoryId,
            @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
            @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude,
            @DecimalMin("0.00") @Digits(integer = 8, fraction = 2) BigDecimal entranceFee,
            @Pattern(regexp = "^[A-Z]{3}$", message = "must be a 3-letter currency code such as USD") String entranceFeeCurrency,
            boolean featured,
            @Min(0) @Max(100000) int sortOrder,
            @NotNull @Valid TranslationBody translation) {
    }

    /** {@code version} must match the current one, otherwise someone else edited it first (409). */
    public record UpdateDestinationRequest(
            @NotBlank @Size(max = 120) @Pattern(regexp = Slugs.PATTERN, message = "must be lower-case words joined by hyphens") String slug,
            @NotNull Long categoryId,
            @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
            @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude,
            @DecimalMin("0.00") @Digits(integer = 8, fraction = 2) BigDecimal entranceFee,
            @Pattern(regexp = "^[A-Z]{3}$", message = "must be a 3-letter currency code such as USD") String entranceFeeCurrency,
            boolean featured,
            @Min(0) @Max(100000) int sortOrder,
            @NotNull Long version) {
    }

    /** The complete image set: one optional cover and an ordered gallery of media ids. */
    public record ImagesRequest(Long cover, @Size(max = 30) List<Long> gallery) {
    }

    public record AdminDestinationSummary(Long id, String slug, String name, Long categoryId, boolean published,
                                          boolean featured, int sortOrder, Instant updatedAt,
                                          List<String> languages) {
    }

    public record AdminDestinationDetail(Long id, long version, String slug, Long categoryId, Double latitude,
                                         Double longitude, BigDecimal entranceFee, String entranceFeeCurrency,
                                         boolean featured, boolean published, int sortOrder, Instant updatedAt,
                                         AdminImages images, Map<String, TranslationBody> translations) {
    }
}
