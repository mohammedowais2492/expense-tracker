package com.expensetracker.ui.alerts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * Tests the JavaFX implementation of the application alert service.
 */
@ExtendWith(ApplicationExtension.class)
class JavaFxAlertServiceTest {

    private JavaFxAlertService alertService;

    @Start
    private void start(Stage stage) {
        stage.setScene(new Scene(new StackPane(), 400, 300));
        stage.show();

        alertService = new JavaFxAlertService(stage);
    }

    @Test
    void returnsTrueWhenConfirmationIsAccepted(
            FxRobot robot) throws Exception {

        CompletableFuture<Boolean> result = new CompletableFuture<>();

        Platform.runLater(() -> {
            try {
                result.complete(alertService.confirm(
                        "Confirmation",
                        "Confirm action",
                        "Do you want to continue?"));
            } catch (Throwable throwable) {
                result.completeExceptionally(throwable);
            }
        });

        WaitForAsyncUtils.waitForFxEvents();

        DialogPane dialogPane = robot.lookup(".dialog-pane")
                .queryAs(DialogPane.class);

        Button okButton = (Button) dialogPane
                .lookupButton(ButtonType.OK);

        robot.clickOn(okButton);

        assertTrue(result.get(5, TimeUnit.SECONDS));
    }

    @Test
    void returnsFalseWhenConfirmationIsCancelled(
            FxRobot robot) throws Exception {

        CompletableFuture<Boolean> result = new CompletableFuture<>();

        Platform.runLater(() -> {
            try {
                result.complete(alertService.confirm(
                        "Confirmation",
                        "Confirm action",
                        "Do you want to continue?"));
            } catch (Throwable throwable) {
                result.completeExceptionally(throwable);
            }
        });

        WaitForAsyncUtils.waitForFxEvents();

        DialogPane dialogPane = robot.lookup(".dialog-pane")
                .queryAs(DialogPane.class);

        Button cancelButton = (Button) dialogPane
                .lookupButton(ButtonType.CANCEL);

        robot.clickOn(cancelButton);

        assertFalse(result.get(5, TimeUnit.SECONDS));
    }

    @Test
    void displaysThemedErrorAlert(FxRobot robot) throws Exception {
        CompletableFuture<Void> completion = new CompletableFuture<>();

        Platform.runLater(() -> {
            try {
                alertService.showError(
                        "Error",
                        "Operation failed",
                        "An unexpected error occurred.");

                completion.complete(null);
            } catch (Throwable throwable) {
                completion.completeExceptionally(throwable);
            }
        });

        WaitForAsyncUtils.waitForFxEvents();

        DialogPane dialogPane = robot.lookup(".dialog-pane")
                .queryAs(DialogPane.class);

        robot.interact(() -> {
            assertEquals("Operation failed",
                    dialogPane.getHeaderText());

            assertEquals("An unexpected error occurred.",
                    dialogPane.getContentText());

            assertTrue(dialogPane.getStylesheets().stream()
                    .anyMatch(path -> path.endsWith("application.css")));

            assertEquals("Error",
                    ((Stage) dialogPane.getScene().getWindow())
                            .getTitle());
        });

        Button okButton = (Button) dialogPane
                .lookupButton(ButtonType.OK);

        robot.clickOn(okButton);

        completion.get(5, TimeUnit.SECONDS);
    }
}
