package com.expensetracker.ui.screens.landing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Tests the Create User dialog controller using JavaFX controls
 * loaded from FXML.
 */
@ExtendWith(ApplicationExtension.class)
class CreateUserDialogControllerTest {

    private CreateUserDialogController controller;

    @Start
    private void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource(
                        "/com/expensetracker/ui/dialogs/create-user.fxml"));

        Parent root = loader.load();
        controller = loader.getController();

        stage.setScene(new Scene(root));
        stage.show();
    }

    @Test
    void initializesControllerWhenFxmlIsLoaded() {
        assertNotNull(controller);
    }

    @Test
    void returnsFirstNameWhenEntered(FxRobot robot) {
        robot.clickOn("#firstNameField").write("firstName");

        assertEquals("firstName", controller.getFirstName());
    }

    @Test
    void returnsLastNameWhenEntered(FxRobot robot) {
        robot.clickOn("#lastNameField").write("lastName");

        assertEquals("lastName", controller.getLastName());
    }

    @Test
    void stripsWhitespaceFromNames(FxRobot robot) {
        robot.clickOn("#firstNameField").write("  firstName  ");
        robot.clickOn("#lastNameField").write("  lastName  ");

        assertEquals("firstName", controller.getFirstName());
        assertEquals("lastName", controller.getLastName());
    }
}
