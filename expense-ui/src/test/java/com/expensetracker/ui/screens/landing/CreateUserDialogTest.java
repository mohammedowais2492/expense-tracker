package com.expensetracker.ui.screens.landing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * Tests the Create User dialog's validation and result handling.
 */
@ExtendWith(ApplicationExtension.class)
class CreateUserDialogTest {

    private CreateUserDialog dialog;
    private Button createButton;
    private Button cancelButton;

    @Start
    private void start(Stage stage) {
        stage.setScene(new Scene(new StackPane(), 400, 300));
        stage.show();

        dialog = new CreateUserDialog();
        dialog.initOwner(stage);

        ButtonType createButtonType = dialog.getDialogPane()
                .getButtonTypes()
                .stream()
                .filter(type -> "Create User".equals(type.getText()))
                .findFirst()
                .orElseThrow();

        createButton = (Button) dialog.getDialogPane()
                .lookupButton(createButtonType);

        cancelButton = (Button) dialog.getDialogPane()
                .lookupButton(ButtonType.CANCEL);

        dialog.show();
    }

    @AfterEach
    void tearDown() {
        WaitForAsyncUtils.waitForAsyncFx(5000, () -> {
            if (dialog != null && dialog.isShowing()) {
                dialog.close();
            }
        });
    }

    @Test
    void disablesCreateButtonWhenFirstNameIsEmpty() throws Exception {
        assertTrue(
                WaitForAsyncUtils.waitForAsyncFx(5000, createButton::isDisabled));
    }

    @Test
    void enablesCreateButtonWhenFirstNameIsValid(
            FxRobot robot) throws Exception {

        robot.clickOn("#firstNameField").write("firstName");

        assertFalse(
                WaitForAsyncUtils.waitForAsyncFx(5000, createButton::isDisabled));
    }

    @Test
    void disablesCreateButtonWhenFirstNameIsWhitespace(
            FxRobot robot) throws Exception {

        robot.clickOn("#firstNameField").write("   ");

        assertTrue(
                WaitForAsyncUtils.waitForAsyncFx(5000, createButton::isDisabled));
    }

    @Test
    void returnsUserInputWhenCreateButtonIsClicked(
            FxRobot robot) throws Exception {

        robot.clickOn("#firstNameField").write("firstName");
        robot.clickOn("#lastNameField").write("lastName");

        robot.clickOn(createButton);

        UserInput result = WaitForAsyncUtils
                .waitForAsyncFx(5000, dialog::getResult);

        assertEquals(
                new UserInput("firstName", "lastName"),
                result);
    }

    @Test
    void returnsNullWhenCancelButtonIsClicked(
            FxRobot robot) throws Exception {

        robot.clickOn(cancelButton);

        UserInput result = WaitForAsyncUtils
                .waitForAsyncFx(5000, dialog::getResult);

        assertNull(result);
    }
}
