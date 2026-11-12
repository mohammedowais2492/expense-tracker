package com.expensetracker.ui.context;

import lombok.Getter;

/**
 * Maintains the currently selected user and expense profile
 * for the Expense Tracker UI.
 *
 * <p>
 * Stores mutable UI session state independently of the application's
 * service dependencies. Selecting a different user clears the
 * previously selected expense profile.
 *
 * <p>
 * This class does not validate user or profile existence, nor does
 * it perform database operations. Such checks belong to the
 * application service and persistence layers.
 */
@Getter
public final class UserContext {
    private Long currentUserId;
    private Long selectedProfileId;

    /**
     * Selects the current user and clears the selected profile.
     *
     * @param userId the user ID; must be positive
     * @throws IllegalArgumentException if userId is not positive
     */
    public void selectUser(long userId) {
        if (userId <= 0) {
            throw new IllegalArgumentException("userId must be positive");
        }
        this.currentUserId = userId;
        this.selectedProfileId = null;
    }

    /**
     * Selects an expense profile for the current user.
     *
     * @param profileId the profile ID; must be positive
     * @throws IllegalArgumentException if profileId is not positive
     * @throws IllegalStateException    if no user is selected
     */
    public void selectProfile(long profileId) {
        if (profileId <= 0) {
            throw new IllegalArgumentException("profileId must be positive");
        }
        if (this.currentUserId == null) {
            throw new IllegalStateException("No user is currently selected");
        }
        this.selectedProfileId = profileId;
    }

    /**
     * Clears the selected profile without changing the current user.
     */
    public void clearProfile() {
        this.selectedProfileId = null;
    }

    /**
     * Clears the current user and selected profile.
     */
    public void clear() {
        this.selectedProfileId = null;
        this.currentUserId = null;
    }
}
