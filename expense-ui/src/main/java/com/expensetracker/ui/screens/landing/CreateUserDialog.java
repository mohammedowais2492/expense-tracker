package com.expensetracker.ui.screens.landing;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Objects;

import javafx.beans.binding.Bindings;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;

/**
 * Dialog used to collect information for creating a new user.
 *
 * <p>
 * Loads the user-creation form from FXML, configures its action
 * buttons, and converts valid form submissions into
 * {@link UserInput} instances.
 *
 * <p>
 * This dialog collects and validates UI input only.
 * It does not create or persist users.
 */
public final class CreateUserDialog extends Dialog<UserInput> {
    /**
     * Constructs the Create User dialog.
     *
     * @throws UncheckedIOException if the dialog FXML cannot be loaded
     */
    public CreateUserDialog() {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/expensetracker/ui/dialogs/create-user.fxml"));

        final Parent content;

        try {
            content = loader.load();
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to load Create User dialog", exception);
        }

        CreateUserDialogController controller = Objects.requireNonNull(loader.getController());

        setTitle("Create User");
        getDialogPane().getStylesheets().add(
                Objects.requireNonNull(
                        getClass().getResource(
                                "/com/expensetracker/ui/styles/application.css"))
                        .toExternalForm());
        getDialogPane().setContent(content);
        ButtonType createButtonType = new ButtonType(
                "Create User",
                ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(createButtonType, ButtonType.CANCEL);

        Node createButton = getDialogPane().lookupButton(createButtonType);

        Node cancelButton = getDialogPane().lookupButton(ButtonType.CANCEL);

        cancelButton.getStyleClass().add("secondary-button");

        createButton.disableProperty().bind(
                Bindings.createBooleanBinding(
                        () -> controller.getFirstName().isBlank(),
                        controller.firstNameProperty()));

        setResultConverter(buttonType -> {
            if (buttonType == createButtonType) {
                return new UserInput(
                        controller.getFirstName(),
                        controller.getLastName());
            }

            return null;
        });
    }
}
