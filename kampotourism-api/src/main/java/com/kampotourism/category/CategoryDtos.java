package com.kampotourism.category;

import com.kampotourism.common.Slugs;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Map;

public final class CategoryDtos {

    private CategoryDtos() {
    }

    /** Public shape. */
    public record CategoryDto(String kind, String slug, String name, String description) {
    }

    public record CategoryTextBody(@NotBlank @Size(max = 100) String name, @Size(max = 500) String description) {
    }

    public record CreateCategoryRequest(
            @NotBlank String kind,
            @NotBlank @Size(max = 80) @Pattern(regexp = Slugs.PATTERN, message = "must be lower-case words joined by hyphens") String slug,
            @Min(0) @Max(100000) int sortOrder,
            @NotNull @Valid CategoryTextBody translation) {
    }

    public record UpdateCategoryRequest(
            @NotBlank @Size(max = 80) @Pattern(regexp = Slugs.PATTERN, message = "must be lower-case words joined by hyphens") String slug,
            @Min(0) @Max(100000) int sortOrder,
            @NotNull Long version) {
    }

    public record AdminCategoryDetail(Long id, long version, String kind, String slug, int sortOrder,
                                      Map<String, CategoryTextBody> translations) {
    }
}
