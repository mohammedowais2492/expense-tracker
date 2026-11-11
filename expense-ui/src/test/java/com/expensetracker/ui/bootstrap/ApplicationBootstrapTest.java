package com.expensetracker.ui.bootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ApplicationBootstrap}.
 *
 * <p>
 * Verifies input validation independently of database initialization.
 */
@DisplayName("ApplicationBootstrap Unit Tests")
class ApplicationBootstrapTest {

    @Test
    @DisplayName("Throws NullPointerException when database path is null")
    void throwsNullPointerExceptionWhenDatabasePathIsNull() {
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> ApplicationBootstrap.initialize(null));

        assertEquals(
                "databasePath must not be null",
                exception.getMessage());
    }
}
