package com.expensetracker.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Unit tests for {@link ExpenseProfile} */
class ExpenseProfileTest {

        @Test
        void buildsSuccessfullyWhenAllFieldsAreSpecified() {
                ExpenseProfile profile = ExpenseProfile.builder()
                                .userId(1L)
                                .profileName("Personal")
                                .build();

                assertNull(profile.getId());
                assertEquals(1L, profile.getUserId());
                assertEquals("Personal", profile.getProfileName());
        }

        @Test
        void throwsNullPointerExceptionWhenProfileNameIsNull() {
                assertThrows(
                                NullPointerException.class,
                                () -> ExpenseProfile.builder()
                                                .userId(1L)
                                                .profileName(null)
                                                .build());
        }

        @ParameterizedTest
        @ValueSource(strings = { "", " ", "   ", "\t", "\n" })
        void throwsIllegalArgumentExceptionWhenProfileNameIsBlank(String profileName) {
                assertThrows(
                                IllegalArgumentException.class,
                                () -> ExpenseProfile.builder()
                                                .userId(1L)
                                                .profileName(profileName)
                                                .build());
        }

        @ParameterizedTest
        @ValueSource(longs = { 0L, -1L })
        void throwsIllegalArgumentExceptionWhenUserIdIsNonPositive(long userId) {
                assertThrows(
                                IllegalArgumentException.class,
                                () -> ExpenseProfile.builder()
                                                .userId(userId)
                                                .profileName("Personal")
                                                .build());
        }

        @Test
        void buildsSuccessfullyWhenProfileNameHasSurroundingWhitespace() {
                ExpenseProfile profile = ExpenseProfile.builder()
                                .userId(1L)
                                .profileName("  Personal  ")
                                .build();

                assertEquals("Personal", profile.getProfileName());
        }

        @Test
        void reconstituteConstructsSuccessfullyWhenAllFieldsAreSpecified() {
                ExpenseProfile profile = ExpenseProfile.reconstitute(
                                1L,
                                2L,
                                "Personal");

                assertEquals(1L, profile.getId());
                assertEquals(2L, profile.getUserId());
                assertEquals("Personal", profile.getProfileName());
        }

        @ParameterizedTest
        @ValueSource(longs = { 0L, -1L })
        void reconstituteThrowsIllegalArgumentExceptionWhenIdIsNonPositive(long id) {
                assertThrows(
                                IllegalArgumentException.class,
                                () -> ExpenseProfile.reconstitute(id, 1L, "Personal"));
        }

        @Test
        void reconstituteThrowsNullPointerExceptionWhenProfileNameIsNull() {
                assertThrows(
                                NullPointerException.class,
                                () -> ExpenseProfile.reconstitute(1L, 2L, null));
        }

        @ParameterizedTest
        @ValueSource(strings = { "", " ", "   ", "\t", "\n" })
        void reconstituteThrowsIllegalArgumentExceptionWhenProfileNameIsBlank(String profileName) {
                assertThrows(
                                IllegalArgumentException.class,
                                () -> ExpenseProfile.reconstitute(1L, 2L, profileName));
        }

        @ParameterizedTest
        @ValueSource(longs = { 0L, -1L })
        void reconstituteThrowsIllegalArgumentExceptionWhenUserIdIsNonPositive(long userId) {
                assertThrows(
                                IllegalArgumentException.class,
                                () -> ExpenseProfile.reconstitute(1L, userId, "Personal"));
        }

        @Test
        void reconstituteTrimsSurroundingWhitespaceFromProfileName() {
                ExpenseProfile profile = ExpenseProfile.reconstitute(
                                1L,
                                2L,
                                "  Personal  ");

                assertEquals("Personal", profile.getProfileName());
        }

        @Test
        void renameIsSuccessful() {
                ExpenseProfile original = ExpenseProfile.reconstitute(
                                1L, 10L, "Personal");

                ExpenseProfile renamed = original.rename("Household");

                assertEquals(1L, renamed.getId());
                assertEquals(10L, renamed.getUserId());
                assertEquals("Household", renamed.getProfileName());

                assertNotSame(original, renamed);
                assertEquals("Personal", original.getProfileName());
        }

        @Test
        void renameIsSuccessfulAndTrimsProfileName() {
                ExpenseProfile original = ExpenseProfile.reconstitute(
                                1L, 10L, "Personal");

                ExpenseProfile renamed = original.rename("  Household  ");

                assertEquals("Household", renamed.getProfileName());
        }

        @Test
        void renameThrowsNullPointerExceptionWhenProfileNameIsNull() {
                ExpenseProfile original = ExpenseProfile.reconstitute(
                                1L, 10L, "Personal");

                assertThrows(
                                NullPointerException.class,
                                () -> original.rename(null));
        }

        @ParameterizedTest
        @ValueSource(strings = { "", " ", "   " })
        void renameThrowsIllegalArgumentExceptionWhenProfileNameIsBlank(String profileName) {

                ExpenseProfile original = ExpenseProfile.reconstitute(
                                1L, 10L, "Personal");

                assertThrows(
                                IllegalArgumentException.class,
                                () -> original.rename(profileName));
        }
}