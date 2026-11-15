package com.expensetracker.ui.screens.landing;

/**
 * Represents user information collected by the Create User dialog.
 *
 * @param firstName the entered first name
 * @param lastName  the entered last name, which may be blank
 */
public record UserInput(String firstName, String lastName) {
}