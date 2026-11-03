
package com.expensetracker.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Unit tests for {@link Page} */
class PageTest {

    @Test
    void buildsSuccessfullyWithContentAndNoNextPage() {
        List<String> content = List.of("A", "B");

        Page<String> page = Page.<String>builder()
                .content(content)
                .pageSize(20)
                .hasNext(false)
                .nextPageToken(null)
                .build();

        assertEquals(content, page.getContent());
        assertEquals(20, page.getPageSize());
        assertEquals(false, page.isHasNext());
        assertEquals(null, page.getNextPageToken());
    }

    @Test
    void buildsSuccessfullyWithContentAndNextPageToken() {
        List<String> content = List.of("A", "B");
        String nextPageToken = "sample-page-token";

        Page<String> page = Page.<String>builder()
                .content(content)
                .pageSize(20)
                .hasNext(true)
                .nextPageToken(nextPageToken)
                .build();

        assertEquals(content, page.getContent());
        assertEquals(20, page.getPageSize());
        assertEquals(true, page.isHasNext());
        assertEquals(nextPageToken, page.getNextPageToken());
    }

    @Test
    void buildsSuccessfullyWithEmptyContent() {
        Page<String> page = Page.<String>builder()
                .content(List.of())
                .pageSize(20)
                .hasNext(false)
                .build();

        assertEquals(List.of(), page.getContent());
    }

    @Test
    void throwsNullPointerExceptionWhenContentIsNull() {
        assertThrows(
                NullPointerException.class,
                () -> Page.<String>builder()
                        .content(null)
                        .pageSize(20)
                        .build());
    }

    @Test
    void throwsNullPointerExceptionWhenContentContainsNull() {
        List<String> content = new ArrayList<>();
        content.add("A");
        content.add(null);

        assertThrows(
                NullPointerException.class,
                () -> Page.<String>builder()
                        .content(content)
                        .pageSize(20)
                        .build());
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, -1 })
    void throwsIllegalArgumentExceptionWhenPageSizeIsNonPositive(int pageSize) {
        assertThrows(
                IllegalArgumentException.class,
                () -> Page.<String>builder()
                        .content(List.of())
                        .pageSize(pageSize)
                        .build());
    }

    @Test
    void throwsIllegalArgumentExceptionWhenContentExceedsPageSize() {
        assertThrows(
                IllegalArgumentException.class,
                () -> Page.<String>builder()
                        .content(List.of("A", "B", "C"))
                        .pageSize(2)
                        .build());
    }

    @Test
    void throwsIllegalArgumentExceptionWhenNextPageTokenIsProvidedButHasNextIsFalse() {
        assertThrows(
                IllegalArgumentException.class,
                () -> Page.<String>builder()
                        .content(List.of("A"))
                        .pageSize(20)
                        .hasNext(false)
                        .nextPageToken("sample-page-token")
                        .build());
    }

    @Test
    void throwsIllegalArgumentExceptionWhenNextPageTokenIsNullButHasNextIsTrue() {
        assertThrows(
                IllegalArgumentException.class,
                () -> Page.<String>builder()
                        .content(List.of("A"))
                        .pageSize(20)
                        .hasNext(true)
                        .nextPageToken(null)
                        .build());
    }

    @Test
    void contentCannotBeModifiedAfterConstruction() {
        Page<String> page = Page.<String>builder()
                .content(List.of("A", "B"))
                .pageSize(20)
                .build();

        assertThrows(
                UnsupportedOperationException.class,
                () -> page.getContent().add("C"));
    }

    @Test
    void changesToOriginalListDoNotAffectPageContent() {
        List<String> originalContent = new ArrayList<>(List.of("A", "B"));

        Page<String> page = Page.<String>builder()
                .content(originalContent)
                .pageSize(20)
                .build();

        originalContent.add("C");

        assertEquals(List.of("A", "B"), page.getContent());
        assertNotSame(originalContent, page.getContent());
    }
}
