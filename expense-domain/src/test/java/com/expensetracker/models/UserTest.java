
package com.expensetracker.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/** Unit tests for {@link User} */
class UserTest {

    private static final long USER_ID = 1L;
    private static final String FIRST_NAME = "FirstName";
    private static final String LAST_NAME = "LastName";

    // Builder: valid construction

    @Test
    void buildsSuccessfullyWithAllFields() {
        User user = User.builder()
                .firstName(FIRST_NAME)
                .lastName(LAST_NAME)
                .build();

        assertEquals(FIRST_NAME, user.getFirstName());
        assertEquals(LAST_NAME, user.getLastName());
        assertNull(user.getId());
    }

    @Test
    void buildsSuccessfullyWithoutLastName() {
        User user = User.builder()
                .firstName(FIRST_NAME)
                .build();

        assertEquals(FIRST_NAME, user.getFirstName());
        assertNull(user.getLastName());
        assertNull(user.getId());
    }

    // Builder: required fields

    @Test
    void throwsNullPointerExceptionWhenFirstNameIsNull() {
        assertThrows(NullPointerException.class, () -> User.builder()
                .firstName(null)
                .lastName(LAST_NAME)
                .build());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { " ", "   ", "\t", "\n" })
    void throwsIllegalArgumentExceptionWhenFirstNameIsBlank(String firstName) {
        // Null is handled by Lombok's @NonNull before domain validation.
        if (firstName == null) {
            return;
        }

        assertThrows(IllegalArgumentException.class, () -> User.builder()
                .firstName(firstName)
                .lastName(LAST_NAME)
                .build());
    }

    // Builder: normalization

    @Test
    void trimsLeadingAndTrailingWhitespaceFromFirstName() {
        User user = User.builder()
                .firstName("  " + FIRST_NAME + "  ")
                .lastName(LAST_NAME)
                .build();

        assertEquals(FIRST_NAME, user.getFirstName());
    }

    @Test
    void trimsLeadingAndTrailingWhitespaceFromLastName() {
        User user = User.builder()
                .firstName(FIRST_NAME)
                .lastName("  " + LAST_NAME + "  ")
                .build();

        assertEquals(LAST_NAME, user.getLastName());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { " ", "   ", "\t", "\n" })
    void normalizesNullOrBlankLastNameToNull(String lastName) {
        User user = User.builder()
                .firstName(FIRST_NAME)
                .lastName(lastName)
                .build();

        assertNull(user.getLastName());
    }

    // Reconstitution: valid construction

    @Test
    void reconstituteConstructsSuccessfullyWithAllFields() {
        User user = User.reconstitute(USER_ID, FIRST_NAME, LAST_NAME);

        assertEquals(USER_ID, user.getId());
        assertEquals(FIRST_NAME, user.getFirstName());
        assertEquals(LAST_NAME, user.getLastName());
    }

    @Test
    void reconstituteConstructsSuccessfullyWithoutLastName() {
        User user = User.reconstitute(USER_ID, FIRST_NAME, null);

        assertEquals(USER_ID, user.getId());
        assertEquals(FIRST_NAME, user.getFirstName());
        assertNull(user.getLastName());
    }

    // Reconstitution: required fields

    @Test
    void reconstituteThrowsNullPointerExceptionWhenFirstNameIsNull() {
        assertThrows(NullPointerException.class, () -> User.reconstitute(USER_ID, null, LAST_NAME));
    }

    @ParameterizedTest
    @ValueSource(strings = { "", " ", "   ", "\t", "\n" })
    void reconstituteThrowsIllegalArgumentExceptionWhenFirstNameIsBlank(String firstName) {
        assertThrows(IllegalArgumentException.class, () -> User.reconstitute(USER_ID, firstName, LAST_NAME));
    }

    // Reconstitution: normalization

    @Test
    void reconstituteTrimsLeadingAndTrailingWhitespaceFromFirstName() {
        User user = User.reconstitute(USER_ID, "  " + FIRST_NAME + "  ", LAST_NAME);

        assertEquals(FIRST_NAME, user.getFirstName());
    }

    @Test
    void reconstituteTrimsLeadingAndTrailingWhitespaceFromLastName() {
        User user = User.reconstitute(USER_ID, FIRST_NAME, "  " + LAST_NAME + "  ");

        assertEquals(LAST_NAME, user.getLastName());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { " ", "   ", "\t", "\n" })
    void reconstituteNormalizesNullOrBlankLastNameToNull(String lastName) {
        User user = User.reconstitute(USER_ID, FIRST_NAME, lastName);

        assertNull(user.getLastName());
    }

    // Reconstitution: persistence ID validation

    @ParameterizedTest
    @ValueSource(longs = { 0L, -1L })
    void reconstituteThrowsIllegalArgumentExceptionWhenIdIsNonPositive(long id) {
        assertThrows(IllegalArgumentException.class, () -> User.reconstitute(id, FIRST_NAME, LAST_NAME));
    }
}
