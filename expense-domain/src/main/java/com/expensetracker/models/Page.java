package com.expensetracker.models;

import java.util.List;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

/**
 * Represents a page of results returned by a paginated query.
 *
 * <p>
 * The content is an immutable copy of the supplied list.
 * The page size represents the maximum number of records allowed
 * in the page.
 * </p>
 *
 * <p>
 * If another page exists, {@code hasNext} must be true and
 * {@code nextPageToken} must contain the continuation token.
 * Otherwise, the continuation token must be null.
 * </p>
 *
 * @param <T> the type of records contained in the page
 */
@Value
public class Page<T> {
    List<T> content;
    int pageSize;
    boolean hasNext;
    String nextPageToken;

    /**
     * Creates a page with the supplied content and pagination metadata.
     *
     * @param content       the records contained in the page
     * @param pageSize      the maximum number of records allowed in the page
     * @param hasNext       whether another page of results exists
     * @param nextPageToken the continuation token, or null if no next page exists
     * @throws NullPointerException     if content or any element in content is null
     * @throws IllegalArgumentException if pageSize is non-positive,
     *                                  content exceeds pageSize, or the
     *                                  continuation token
     *                                  is inconsistent with hasNext
     */
    @Builder
    public Page(@NonNull List<T> content, int pageSize, boolean hasNext, String nextPageToken) {
        if (pageSize <= 0) {
            throw new IllegalArgumentException("pageSize must be positive");
        }
        if (content.size() > pageSize) {
            throw new IllegalArgumentException("content size cannot exceed pageSize");
        }
        this.content = List.copyOf(content);
        this.pageSize = pageSize;
        if (!hasNext && nextPageToken != null) {
            throw new IllegalArgumentException("nextPageToken must be null when hasNext is false");
        }
        if (hasNext && nextPageToken == null) {
            throw new IllegalArgumentException("nextPageToken cannot be null when hasNext is true");
        }
        this.hasNext = hasNext;
        this.nextPageToken = nextPageToken;
    }
}
