package com.kampotourism.common;

public final class Slugs {

    private Slugs() {
    }

    /** lower-case words separated by single hyphens, e.g. "pepper-country". */
    public static final String PATTERN = "^[a-z0-9]+(?:-[a-z0-9]+)*$";
}
