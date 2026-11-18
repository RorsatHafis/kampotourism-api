package com.kampotourism.category;

import com.kampotourism.common.error.BadRequestException;
import java.util.Arrays;
import java.util.Locale;

/** Which part of the site a category belongs to. */
public enum CategoryKind {
    DESTINATION, EXPERIENCE, STAY_TYPE, TRANSPORT, BLOG, TRAVEL_INFO;

    /** URL/JSON form, e.g. "stay-type". */
    public String key() {
        return name().toLowerCase(Locale.ROOT).replace('_', '-');
    }

    public static CategoryKind fromKey(String key) {
        String wanted = key == null ? "" : key.trim();
        for (CategoryKind kind : values()) {
            if (kind.key().equalsIgnoreCase(wanted)) {
                return kind;
            }
        }
        throw new BadRequestException("Unknown category kind '%s'. Allowed values: %s".formatted(
                key, Arrays.stream(values()).map(CategoryKind::key).toList()));
    }
}
