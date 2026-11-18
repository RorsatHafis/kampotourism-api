package com.kampotourism.content;

import com.fasterxml.jackson.databind.JsonNode;
import com.kampotourism.category.CategoryDtos.CategoryDto;
import com.kampotourism.media.MediaDtos.ImageDto;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

/** Request/response contracts shared by experiences, accommodation, transportation, articles and travel information. */
public final class ContentDtos {
    private ContentDtos() { }
    public record Coordinates(Double latitude, Double longitude) { }
    public record Price(BigDecimal amount, String currency, String unit) { }
    public record ContentCard(String slug, String title, String excerpt, CategoryDto category, ImageDto cover,
                              List<String> tags, String bestFor, boolean featured, Instant publishedAt, String language) { }
    public record Seo(String title, String description, String canonical, ImageDto image) { }
    public record ContentDetail(String slug, String title, String excerpt, JsonNode body, JsonNode highlights, JsonNode tips,
                                CategoryDto category, Coordinates location, String address, Price price,
                                String contactPhone, String contactEmail, String externalUrl, JsonNode metadata,
                                List<String> tags, String bestFor, String authorName, String authorUrl,
                                boolean confirmedInformation, ImageDto cover, List<ImageDto> gallery, Seo seo,
                                Instant publishedAt, String language) { }
    public record TranslationBody(@NotBlank @Size(max=250) String title, @Size(max=700) String excerpt,
                                  @NotNull JsonNode body, @NotNull JsonNode highlights, @NotNull JsonNode tips,
                                  @Size(max=250) String seoTitle, @Size(max=500) String seoDescription,
                                  boolean confirmedInformation) { }
    public record CreateRequest(@NotBlank @Size(max=120) @Pattern(regexp="^[a-z0-9]+(?:-[a-z0-9]+)*$") String slug,
                                Long categoryId, boolean featured, @Min(0) @Max(100000) int sortOrder,
                                @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
                                @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude,
                                @DecimalMin("0.00") @Digits(integer=8,fraction=2) BigDecimal price,
                                @Pattern(regexp="^[A-Z]{3}$") String currency, @Size(max=60) String pricingUnit,
                                @Size(max=500) String address, @Size(max=80) String contactPhone,
                                @Email @Size(max=254) String contactEmail, @Pattern(regexp="^https?://[^\\s]{1,993}$", message="must be an http(s) URL") String externalUrl,
                                @Size(max=20) List<@NotBlank @Size(max=80) String> tags, @Size(max=200) String bestFor,
                                @Size(max=160) String authorName, @Pattern(regexp="^https?://[^\\s]{1,993}$", message="must be an http(s) URL") String authorUrl,
                                @NotNull JsonNode metadata, @NotNull @Valid TranslationBody translation) { }
    public record UpdateRequest(@NotBlank @Size(max=120) @Pattern(regexp="^[a-z0-9]+(?:-[a-z0-9]+)*$") String slug,
                                Long categoryId, boolean featured, @Min(0) @Max(100000) int sortOrder,
                                @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
                                @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude,
                                @DecimalMin("0.00") @Digits(integer=8,fraction=2) BigDecimal price,
                                @Pattern(regexp="^[A-Z]{3}$") String currency, @Size(max=60) String pricingUnit,
                                @Size(max=500) String address, @Size(max=80) String contactPhone,
                                @Email @Size(max=254) String contactEmail, @Pattern(regexp="^https?://[^\\s]{1,993}$", message="must be an http(s) URL") String externalUrl,
                                @Size(max=20) List<@NotBlank @Size(max=80) String> tags, @Size(max=200) String bestFor,
                                @Size(max=160) String authorName, @Pattern(regexp="^https?://[^\\s]{1,993}$", message="must be an http(s) URL") String authorUrl,
                                @NotNull JsonNode metadata, @NotNull Long version) { }
    public record ImagesRequest(Long cover, @Size(max=30) List<Long> gallery) { }
    public record AdminSummary(Long id, String slug, ContentStatus status, boolean featured, Long categoryId,
                               String title, List<String> languages, Instant updatedAt) { }
    public record AdminDetail(Long id, long version, String slug, ContentStatus status, boolean featured, int sortOrder,
                              Long categoryId, Double latitude, Double longitude, BigDecimal price, String currency,
                              String pricingUnit, String address, String contactPhone, String contactEmail, String externalUrl,
                              List<String> tags, String bestFor, String authorName, String authorUrl, JsonNode metadata,
                              Instant publishedAt, Instant updatedAt, com.kampotourism.media.MediaDtos.AdminImages images,
                              Map<String, TranslationBody> translations) { }
}
