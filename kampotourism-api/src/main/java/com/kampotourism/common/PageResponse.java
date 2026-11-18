package com.kampotourism.common;

import java.util.List;

/** Pagination envelope. {@code page} is 1-based to match the frontend. */
public record PageResponse<T>(List<T> items, int page, int size, long totalItems, int totalPages) {
}
