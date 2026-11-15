package com.expensetracker.ui.screens.landing;

import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;

/**
 * Controls the input form used to create a new user.
 *
 * <p>
 * Manages the first-name and last-name text fields declared in
 * the Create User dialog's FXML.
 *
 * <p>
 * This controller is responsible only for collecting form input.
 * User creation and persistence are coordinated by the landing
 * screen controller through the user service.
 */
public final class CreateUserDialogController {

    @FXML
    private TextField firstNameField;
    @FXML
    private TextField lastNameField;

    /**
     * Returns the first name entered in the form.
     *
     * @return the first name with surrounding whitespace removed
     */
    public String getFirstName() {
        return firstNameField.getText().strip();
    }

    /**
     * Returns the last name entered in the form.
     *
     * @return the last name with surrounding whitespace removed
     */
    public String getLastName() {
        return lastNameField.getText().strip();
    }

    /**
     * Returns the observable text property of the first-name field.
     *
     * @return the first-name text property
     */
    public StringProperty firstNameProperty() {
        return firstNameField.textProperty();
    }
}
