package com.expensetracker.ui.profile;

import com.expensetracker.models.ExpenseProfile;
import com.expensetracker.services.ExpenseProfileService;
import com.expensetracker.ui.context.UserContext;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * Manages expense profile selection for the current application session.
 *
 * <p>
 * Coordinates with {@link ExpenseProfileService} to retrieve an
 * existing expense profile or create a default profile when none exists.
 * The selected profile is stored in {@link UserContext}.
 *
 * <p>
 * This service is independent of JavaFX controls and can be used
 * by different UI controllers to establish the active expense profile.
 */
@RequiredArgsConstructor
public final class ProfileService {
    @NonNull
    private final ExpenseProfileService expenseProfileService;
    @NonNull
    private final UserContext userContext;

    /**
     * Selects the default expense profile for the currently selected user.
     *
     * <p>
     * If an expense profile already exists for the user, it is selected.
     * Otherwise, a new profile named "Default" is created and selected.
     *
     * <p>
     * The selected profile's identifier is stored in
     * {@link UserContext}.
     *
     * @throws IllegalStateException if no user is currently selected
     */
    public void selectDefaultProfile() {
        Long userId = userContext.getCurrentUserId();
        if (userId == null) {
            throw new IllegalStateException("Cannot select profile before a user is selected");
        }
        ExpenseProfile expenseProfile = expenseProfileService.getProfileByUserId(userId)
                .orElseGet(() -> createDefaultProfile(userId));
        userContext.selectProfile(expenseProfile.getId());
    }

    private ExpenseProfile createDefaultProfile(long userId) {
        return expenseProfileService
                .createProfile(ExpenseProfile.builder().profileName("Default").userId(userId).build());
    }
}
