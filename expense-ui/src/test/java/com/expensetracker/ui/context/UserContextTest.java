package com.expensetracker.ui.context;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Unit tests for {@link UserContext}.
 *
 * <p>
 * Verifies user and profile selection, state transitions,
 * input validation, and clearing behavior.
 */
@DisplayName("UserContext Tests")
class UserContextTest {

    private UserContext userContext;

    @BeforeEach
    void setUp() {
        userContext = new UserContext();
    }

    @Test
    @DisplayName("Initializes with no selected user or profile")
    void initializesWithNoSelectedUserOrProfile() {
        assertNull(userContext.getCurrentUserId());
        assertNull(userContext.getSelectedProfileId());
    }

    @Test
    @DisplayName("Selects a valid user")
    void selectsValidUser() {
        userContext.selectUser(1L);

        assertEquals(1L, userContext.getCurrentUserId());
        assertNull(userContext.getSelectedProfileId());
    }

    @Test
    @DisplayName("Clears selected profile when switching users")
    void clearsSelectedProfileWhenSwitchingUsers() {
        userContext.selectUser(1L);
        userContext.selectProfile(10L);

        userContext.selectUser(2L);

        assertEquals(2L, userContext.getCurrentUserId());
        assertNull(userContext.getSelectedProfileId());
    }

    @Test
    @DisplayName("Clears selected profile when reselecting the same user")
    void clearsSelectedProfileWhenReselectingSameUser() {
        userContext.selectUser(1L);
        userContext.selectProfile(10L);

        userContext.selectUser(1L);

        assertEquals(1L, userContext.getCurrentUserId());
        assertNull(userContext.getSelectedProfileId());
    }

    @ParameterizedTest
    @ValueSource(longs = { 0L, -1L })
    @DisplayName("Throws IllegalArgumentException when user ID is not positive")
    void throwsIllegalArgumentExceptionWhenUserIdIsNotPositive(long userId) {
        assertThrows(
                IllegalArgumentException.class,
                () -> userContext.selectUser(userId));

        assertNull(userContext.getCurrentUserId());
    }

    @Test
    @DisplayName("Preserves existing selections when selecting invalid user")
    void preservesExistingSelectionsWhenSelectingInvalidUser() {
        userContext.selectUser(1L);
        userContext.selectProfile(10L);

        assertThrows(
                IllegalArgumentException.class,
                () -> userContext.selectUser(-2L));

        assertEquals(1L, userContext.getCurrentUserId());
        assertEquals(10L, userContext.getSelectedProfileId());
    }

    @Test
    @DisplayName("Selects valid profile when user is selected")
    void selectsValidProfileWhenUserIsSelected() {
        userContext.selectUser(1L);

        userContext.selectProfile(10L);

        assertEquals(1L, userContext.getCurrentUserId());
        assertEquals(10L, userContext.getSelectedProfileId());
    }

    @Test
    @DisplayName("Allows switching between profiles")
    void allowsSwitchingBetweenProfiles() {
        userContext.selectUser(1L);
        userContext.selectProfile(10L);

        userContext.selectProfile(20L);

        assertEquals(1L, userContext.getCurrentUserId());
        assertEquals(20L, userContext.getSelectedProfileId());
    }

    @Test
    @DisplayName("Throws IllegalStateException when selecting profile without user")
    void throwsIllegalStateExceptionWhenSelectingProfileWithoutUser() {
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> userContext.selectProfile(10L));

        assertEquals(
                "No user is currently selected",
                exception.getMessage());

        assertNull(userContext.getCurrentUserId());
        assertNull(userContext.getSelectedProfileId());
    }

    @ParameterizedTest
    @ValueSource(longs = { 0L, -1L })
    @DisplayName("Throws IllegalArgumentException when profile ID is not positive")
    void throwsIllegalArgumentExceptionWhenProfileIdIsNotPositive(long profileId) {
        userContext.selectUser(1L);

        assertThrows(
                IllegalArgumentException.class,
                () -> userContext.selectProfile(profileId));

        assertNull(userContext.getSelectedProfileId());
    }

    @Test
    @DisplayName("Preserves selected profile when invalid profile is supplied")
    void preservesSelectedProfileWhenInvalidProfileIsSupplied() {
        userContext.selectUser(1L);
        userContext.selectProfile(10L);

        assertThrows(
                IllegalArgumentException.class,
                () -> userContext.selectProfile(-5L));

        assertEquals(1L, userContext.getCurrentUserId());
        assertEquals(10L, userContext.getSelectedProfileId());
    }

    @Test
    @DisplayName("Clears profile without clearing user")
    void clearsProfileWithoutClearingUser() {
        userContext.selectUser(1L);
        userContext.selectProfile(10L);

        userContext.clearProfile();

        assertEquals(1L, userContext.getCurrentUserId());
        assertNull(userContext.getSelectedProfileId());
    }

    @Test
    @DisplayName("Clears both user and profile")
    void clearsBothUserAndProfile() {
        userContext.selectUser(1L);
        userContext.selectProfile(10L);

        userContext.clear();

        assertNull(userContext.getCurrentUserId());
        assertNull(userContext.getSelectedProfileId());
    }

    @Test
    @DisplayName("Allows clearing an already empty context")
    void allowsClearingAlreadyEmptyContext() {
        userContext.clearProfile();
        userContext.clear();

        assertNull(userContext.getCurrentUserId());
        assertNull(userContext.getSelectedProfileId());
    }

    @Test
    @DisplayName("Allows selecting a new user after clearing context")
    void allowsSelectingNewUserAfterClearingContext() {
        userContext.selectUser(1L);
        userContext.selectProfile(10L);
        userContext.clear();

        userContext.selectUser(2L);
        userContext.selectProfile(20L);

        assertEquals(2L, userContext.getCurrentUserId());
        assertEquals(20L, userContext.getSelectedProfileId());
    }
}
