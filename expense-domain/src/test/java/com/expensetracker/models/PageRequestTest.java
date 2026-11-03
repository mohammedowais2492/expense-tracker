
package com.expensetracker.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Unit tests for {@link PageRequest} */
class PageRequestTest {

    @Test
    void buildsSuccessfullyWithNullPageToken() {
        PageRequest pageRequest = PageRequest.builder()
                .pageSize(20)
                .pageToken(null)
                .build();

        assertEquals(20, pageRequest.getPageSize());
        assertNull(pageRequest.getPageToken());
    }

    @Test
    void buildsSuccessfullyWithNonNullPageToken() {
        String pageToken = "sample-page-token";

        PageRequest pageRequest = PageRequest.builder()
                .pageSize(20)
                .pageToken(pageToken)
                .build();

        assertEquals(20, pageRequest.getPageSize());
        assertEquals(pageToken, pageRequest.getPageToken());
    }

    @Test
    void buildsSuccessfullyWithMinimumPageSize() {
        PageRequest pageRequest = PageRequest.builder()
                .pageSize(1)
                .build();

        assertEquals(1, pageRequest.getPageSize());
    }

    @Test
    void buildsSuccessfullyWithMaximumPageSize() {
        PageRequest pageRequest = PageRequest.builder()
                .pageSize(100)
                .build();

        assertEquals(100, pageRequest.getPageSize());
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, -1, 101 })
    void throwsIllegalArgumentExceptionWhenPageSizeIsInvalid(int pageSize) {
        assertThrows(
                IllegalArgumentException.class,
                () -> PageRequest.builder()
                        .pageSize(pageSize)
                        .build());
    }
}
