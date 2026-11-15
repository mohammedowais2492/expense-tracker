package com.expensetracker.ui.converters;

import com.expensetracker.models.User;

import javafx.util.StringConverter;

/**
 * Converts {@link User} objects into human-readable display names
 * for JavaFX selection controls.
 *
 * <p>
 * Displays the user's first name and, when available, last name.
 * Database-generated user IDs are not included in the displayed text.
 *
 * <p>
 * Intended for non-editable selection controls. Conversion from
 * a string back into a User is not supported because display
 * names are not guaranteed to be unique.
 */
public final class UserStringConverter extends StringConverter<User> {

    @Override
    public User fromString(String user) {
        throw new UnsupportedOperationException(
                "Conversion from display name to User is not supported");
    }

    @Override
    public String toString(User user) {
        if (user == null) {
            return "";
        }
        String lastName = user.getLastName();
        return lastName == null
                ? user.getFirstName()
                : user.getFirstName() + " " + lastName;
    }
}
