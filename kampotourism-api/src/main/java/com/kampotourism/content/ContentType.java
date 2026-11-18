package com.kampotourism.content;

import com.kampotourism.category.CategoryKind;
import com.kampotourism.common.error.BadRequestException;
import com.kampotourism.media.OwnerType;
import java.util.Arrays;

/** The five independently managed, non-itinerary editorial content modules. */
public enum ContentType {
    EXPERIENCE("experiences", CategoryKind.EXPERIENCE, OwnerType.EXPERIENCE),
    ACCOMMODATION("accommodations", CategoryKind.STAY_TYPE, OwnerType.ACCOMMODATION),
    TRANSPORTATION("transportation", CategoryKind.TRANSPORT, OwnerType.TRANSPORTATION),
    ARTICLE("articles", CategoryKind.BLOG, OwnerType.ARTICLE),
    TRAVEL_INFO("travel-information", CategoryKind.TRAVEL_INFO, OwnerType.TRAVEL_INFO);

    private final String path;
    private final CategoryKind categoryKind;
    private final OwnerType ownerType;

    ContentType(String path, CategoryKind categoryKind, OwnerType ownerType) {
        this.path = path;
        this.categoryKind = categoryKind;
        this.ownerType = ownerType;
    }
    public String path() { return path; }
    public CategoryKind categoryKind() { return categoryKind; }
    public OwnerType ownerType() { return ownerType; }
    public static ContentType fromPath(String path) {
        return Arrays.stream(values()).filter(v -> v.path.equalsIgnoreCase(path)).findFirst()
                .orElseThrow(() -> new BadRequestException("Unknown content module '" + path + "'"));
    }
}
