package com.expensetracker.ui.converters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.expensetracker.models.User;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests the conversion of users into display names.
 */
class UserStringConverterTest {

    private UserStringConverter converter;

    @BeforeEach
    void setUp() {
        converter = new UserStringConverter();
    }

    @Test
    void returnsEmptyStringWhenUserIsNull() {
        assertEquals("", converter.toString(null));
    }

    @Test
    void returnsFirstNameWhenLastNameIsNull() {
        User user = User.builder()
                .firstName("firstName")
                .build();

        assertEquals("firstName", converter.toString(user));
    }

    @Test
    void returnsFullNameWhenLastNameIsPresent() {
        User user = User.builder()
                .firstName("firstName")
                .lastName("lastName")
                .build();

        assertEquals("firstName lastName", converter.toString(user));
    }

    @Test
    void throwsUnsupportedOperationExceptionWhenConvertingFromString() {
        assertThrows(
                UnsupportedOperationException.class,
                () -> converter.fromString("firstName lastName"));
    }
}
