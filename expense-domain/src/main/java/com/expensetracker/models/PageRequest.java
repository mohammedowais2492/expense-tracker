package com.expensetracker.models;

import lombok.Builder;
import lombok.Value;

/**
 * Represents a request for a page of results.
 *
 * <p>
 * The page token is an opaque continuation token used to retrieve
 * subsequent pages. A {@code null} token indicates that the request
 * is for the first page.
 * </p>
 *
 * <p>
 * The requested page size must be between 1 and
 * {@value #MAX_PAGE_SIZE}, inclusive.
 * </p>
 */
@Value
public class PageRequest {
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;

    int pageSize;
    String pageToken;

    /**
     * Creates a page request with the specified page size and continuation token.
     *
     * @param pageSize  the maximum number of records requested
     * @param pageToken the continuation token, or {@code null} for the first page
     * @throws IllegalArgumentException if the page size is outside the allowed
     *                                  range
     */
    @Builder
    public PageRequest(int pageSize, String pageToken) {
        if (pageSize <= 0 || pageSize > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException(
                    String.format("pageSize must be within the range 1-%s (inclusive)", MAX_PAGE_SIZE));
        }
        this.pageSize = pageSize;
        this.pageToken = pageToken;
    }
}
