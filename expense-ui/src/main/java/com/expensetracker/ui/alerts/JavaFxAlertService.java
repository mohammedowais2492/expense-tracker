package com.expensetracker.ui.alerts;

import java.util.Objects;
import java.util.Optional;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Window;
import javafx.scene.control.Alert.AlertType;

/**
 * JavaFX implementation of {@link AlertService}.
 *
 * <p>
 * Displays application alerts using JavaFX dialogs and applies
 * the application's shared visual theme.
 *
 * <p>
 * Confirmation dialogs return {@code true} only when the user
 * explicitly selects the OK action.
 */
public final class JavaFxAlertService implements AlertService {
    private static final String STYLESHEET_PATH = "/com/expensetracker/ui/styles/application.css";
    private final Window owner;

    /**
     * Creates the alert service associated with an application window.
     *
     * @param owner the owning application window
     */
    public JavaFxAlertService(Window owner) {
        this.owner = Objects.requireNonNull(owner);
    }

    @Override
    public void showError(String title, String header, String message) {
        Alert alert = createAlert(AlertType.ERROR, title, header, message);
        alert.showAndWait();
    }

    @Override
    public boolean confirm(String title, String header, String message) {
        Alert alert = createAlert(AlertType.CONFIRMATION, title, header, message);

        Optional<ButtonType> result = alert.showAndWait();

        return result.filter(ButtonType.OK::equals).isPresent();
    }

    private Alert createAlert(
            AlertType type,
            String title,
            String header,
            String message) {
        Alert alert = new Alert(type);
        alert.initOwner(owner);

        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(message);

        alert.getDialogPane().getStylesheets().add(
                Objects.requireNonNull(
                        getClass().getResource(STYLESHEET_PATH))
                        .toExternalForm());

        return alert;
    }
}
