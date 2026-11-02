package com.expensetracker.models;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

/**
 * Represents a user of the expense tracking application.
 *
 * <p>
 * A user must have a nonblank first name. The last name is optional.
 * Leading and trailing whitespace is removed from both names, and a blank
 * last name is normalized to {@code null}.
 *
 * <p>
 * A newly created user has no persistence ID. Users retrieved from
 * persistence can be reconstructed using
 * {@link #reconstitute(long, String, String)}.
 */
@Value
public class User {
    Long id;
    String firstName;
    String lastName;

    /**
     * Creates a new user.
     *
     * <p>
     * The user is initially unpersisted and therefore has no ID.
     *
     * @param firstName the required first name; must not be {@code null} or blank
     * @param lastName  the optional last name; blank values are normalized to
     *                  {@code null}
     * @throws NullPointerException     if {@code firstName} is {@code null}
     * @throws IllegalArgumentException if {@code firstName} is blank after trimming
     */
    @Builder
    public User(@NonNull String firstName, String lastName) {
        this(null, firstName, lastName);
    }

    private User(Long id, String firstName, String lastName) {
        if (id != null && id <= 0) {
            throw new IllegalArgumentException("id cannot be non-positive");
        }
        this.id = id;
        firstName = firstName.strip();
        if (firstName.isBlank()) {
            throw new IllegalArgumentException("firstName cannot be blank");
        }
        this.firstName = firstName;
        if (lastName != null) {
            lastName = lastName.strip();
            if (lastName.isBlank()) {
                lastName = null;
            }
        }
        this.lastName = lastName;
    }

    /**
     * Reconstructs a user using data retrieved from persistence.
     *
     * <p>
     * Unlike the builder, this factory method accepts an existing persistence ID.
     * The same name normalization and validation rules applied during new user
     * creation are enforced here.
     *
     * @param id        the positive persistence ID of the user
     * @param firstName the required first name; must not be {@code null} or blank
     * @param lastName  the optional last name; blank values are normalized to
     *                  {@code null}
     * @return a reconstructed user
     * @throws IllegalArgumentException if {@code id} is nonpositive or
     *                                  {@code firstName} is blank after trimming
     * @throws NullPointerException     if {@code firstName} is {@code null}
     */
    public static User reconstitute(long id, @NonNull String firstName, String lastName) {
        return new User(id, firstName, lastName);
    }
}
