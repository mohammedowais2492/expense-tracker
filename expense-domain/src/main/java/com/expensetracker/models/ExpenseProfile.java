package com.expensetracker.models;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

/**
 * Represents a financial profile owned by a user.
 *
 * <p>
 * A profile has a persistence-generated identifier, an owning user identifier,
 * and a required name. Profile names are trimmed of surrounding whitespace
 * during construction.
 *
 * <p>
 * Profile name uniqueness is enforced outside this model, as it depends on
 * other profiles belonging to the same user.
 */
@Value
public class ExpenseProfile {
    Long id;
    long userId;
    String profileName;

    /**
     * Creates a new financial profile.
     *
     * @param userId      the identifier of the user who owns the profile; must be
     *                    positive
     * @param profileName the profile name; must not be null or blank
     * @throws NullPointerException     if profileName is null
     * @throws IllegalArgumentException if userId is non-positive or profileName is
     *                                  blank
     */
    @Builder
    public ExpenseProfile(long userId, @NonNull String profileName) {
        this(null, userId, profileName);
    }

    private ExpenseProfile(Long id, long userId, String profileName) {
        if (id != null && id <= 0) {
            throw new IllegalArgumentException("id cannot be non-positive");
        }
        this.id = id;
        if (userId <= 0) {
            throw new IllegalArgumentException("userId cannot be non-positive");
        }
        this.userId = userId;
        profileName = profileName.strip();
        if (profileName.isBlank()) {
            throw new IllegalArgumentException("profileName cannot be blank");
        }
        this.profileName = profileName;
    }

    /**
     * Reconstitutes an existing financial profile from persisted data.
     *
     * @param id          the persisted profile identifier; must be positive
     * @param userId      the identifier of the user who owns the profile; must be
     *                    positive
     * @param profileName the profile name; must not be null or blank
     * @return the reconstituted financial profile
     * @throws NullPointerException     if profileName is null
     * @throws IllegalArgumentException if id or userId is non-positive, or
     *                                  profileName is blank
     */
    public static ExpenseProfile reconstitute(long id, long userId, @NonNull String profileName) {
        return new ExpenseProfile(id, userId, profileName);
    }

    /**
     * Creates a new expense profile with an updated name.
     *
     * <p>
     * The returned profile preserves the current profile's ID and associated
     * user ID. The original instance remains unchanged.
     * </p>
     *
     * @param newProfileName the new name for the expense profile
     * @return a new expense profile with the updated name
     * @throws NullPointerException     if {@code newProfileName} is null
     * @throws IllegalArgumentException if {@code newProfileName} is blank
     */
    public ExpenseProfile rename(@NonNull String newProfileName) {
        return new ExpenseProfile(this.id, this.userId, newProfileName);
    }
}
