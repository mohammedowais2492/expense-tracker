package com.expensetracker.ui.screens.landing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import com.expensetracker.models.User;
import com.expensetracker.services.UserService;
import com.expensetracker.ui.alerts.AlertService;
import com.expensetracker.ui.context.UserContext;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * Tests the landing screen's initialization, user selection,
 * and interactions with application services.
 */
@ExtendWith(ApplicationExtension.class)
class LandingControllerTest {

    private final UserService userService = mock(UserService.class);
    private final AlertService alertService = mock(AlertService.class);
    private final UserContext userContext = new UserContext();

    private Stage stage;

    @Start
    private void start(Stage stage) {
        this.stage = stage;

        stage.setScene(new Scene(new StackPane(), 1000, 650));
        stage.show();
    }

    /**
     * Loads the landing screen with the supplied users.
     *
     * @param users users returned by the mocked service
     */
    private void loadLanding(List<User> users) {
        when(userService.getAllUsers()).thenReturn(users);

        WaitForAsyncUtils.waitForAsyncFx(5000, () -> {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(
                            "/com/expensetracker/ui/screens/landing.fxml"));

            loader.setControllerFactory(controllerClass -> {
                if (controllerClass == LandingController.class) {
                    return new LandingController(
                            userService,
                            userContext,
                            alertService);
                }

                throw new IllegalArgumentException(
                        "Unsupported controller: " + controllerClass);
            });

            try {
                Parent root = loader.load();
                stage.setScene(new Scene(root, 1000, 650));
            } catch (IOException exception) {
                throw new IllegalStateException(
                        "Unable to load landing screen", exception);
            }

            return null;
        });
    }

    @Test
    void displaysEmptyStateWhenNoUsersExist(FxRobot robot) {
        loadLanding(List.of());

        Label statusLabel = robot.lookup("#statusLabel")
                .queryAs(Label.class);

        assertEquals(
                "No users found. Create a user to get started.",
                statusLabel.getText());
    }

    @Test
    void populatesUserComboBoxWhenUsersExist(FxRobot robot) {
        User firstUser = User.builder()
                .firstName("firstName")
                .build();

        User secondUser = User.builder()
                .firstName("secondName")
                .build();

        loadLanding(List.of(firstUser, secondUser));

        ComboBox<?> comboBox = robot.lookup("#userComboBox")
                .queryAs(ComboBox.class);

        assertEquals(2, comboBox.getItems().size());
        assertEquals(firstUser, comboBox.getItems().get(0));
        assertEquals(secondUser, comboBox.getItems().get(1));
    }

    @Test
    void disablesContinueButtonWhenNoUserIsSelected(FxRobot robot) {
        User user = User.builder()
                .firstName("firstName")
                .build();

        loadLanding(List.of(user));

        Button continueButton = robot.lookup("#continueButton")
                .queryAs(Button.class);

        assertTrue(continueButton.isDisabled());
    }

    @Test
    void enablesContinueButtonWhenUserIsSelected(FxRobot robot) {
        User user = User.builder()
                .firstName("firstName")
                .build();

        loadLanding(List.of(user));

        @SuppressWarnings("unchecked")
        ComboBox<User> comboBox = robot.lookup("#userComboBox")
                .queryAs(ComboBox.class);

        Button continueButton = robot.lookup("#continueButton")
                .queryAs(Button.class);

        robot.interact(() -> comboBox.getSelectionModel().select(user));

        assertFalse(continueButton.isDisabled());
    }

    @Test
    void selectsUserInContextWhenContinueIsClicked(FxRobot robot) {
        User user = User.reconstitute(
                1L,
                "firstName",
                "lastName");

        loadLanding(List.of(user));

        ComboBox<?> comboBox = robot.lookup("#userComboBox")
                .queryAs(ComboBox.class);

        Button continueButton = robot.lookup("#continueButton")
                .queryAs(Button.class);

        robot.interact(() -> comboBox.getSelectionModel().select(0));

        robot.clickOn(continueButton);

        assertEquals(1L, userContext.getCurrentUserId());
    }

    @Test
    void createsUserAndSelectsPersistedUser(FxRobot robot) {
        User persistedUser = mock(User.class);

        when(persistedUser.getId()).thenReturn(1L);
        when(persistedUser.getFirstName()).thenReturn("firstName");
        when(persistedUser.getLastName()).thenReturn("lastName");

        when(userService.getAllUsers())
                .thenReturn(List.of())
                .thenReturn(List.of(persistedUser));

        when(userService.addUser(any(User.class)))
                .thenReturn(persistedUser);

        // Initializes the screen with no users.
        loadLanding(List.of());

        // Configures the subsequent refresh.
        when(userService.getAllUsers())
                .thenReturn(List.of(persistedUser));

        // Open the Create User dialog from the landing screen.
        robot.clickOn("Create User");

        robot.clickOn("#firstNameField").write("firstName");
        robot.clickOn("#lastNameField").write("lastName");

        // Retrieve the dialog's Create User button.
        DialogPane dialogPane = robot.lookup(".dialog-pane")
                .queryAs(DialogPane.class);

        Button createButton = (Button) dialogPane.getButtonTypes()
                .stream()
                .filter(type -> "Create User".equals(type.getText()))
                .findFirst()
                .map(dialogPane::lookupButton)
                .orElseThrow();

        robot.clickOn(createButton);

        // Verify that the service received the entered information.
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

        verify(userService).addUser(userCaptor.capture());

        assertEquals(
                "firstName",
                userCaptor.getValue().getFirstName());

        assertEquals(
                "lastName",
                userCaptor.getValue().getLastName());

        // Verify that the persisted user is selected.
        ComboBox<?> comboBox = robot.lookup("#userComboBox")
                .queryAs(ComboBox.class);

        robot.interact(() -> {
            assertEquals(1, comboBox.getItems().size());
            assertSame(persistedUser, comboBox.getItems().get(0));
            assertSame(
                    persistedUser,
                    comboBox.getSelectionModel().getSelectedItem());
        });
    }

    @Test
    void displaysErrorWhenUserCreationFails(FxRobot robot) {
        loadLanding(List.of());

        when(userService.addUser(any(User.class)))
                .thenThrow(new IllegalStateException("Database unavailable"));

        robot.clickOn("Create User");

        robot.clickOn("#firstNameField").write("firstName");

        DialogPane dialogPane = robot.lookup(".dialog-pane")
                .queryAs(DialogPane.class);

        Button createButton = (Button) dialogPane.getButtonTypes()
                .stream()
                .filter(type -> "Create User".equals(type.getText()))
                .findFirst()
                .map(dialogPane::lookupButton)
                .orElseThrow();

        robot.clickOn(createButton);

        verify(alertService).showError(
                "User Creation Failed",
                "Unable to create user",
                "An unexpected error occurred while saving the user");

        // A failed save must not trigger a list refresh.
        verify(userService).getAllUsers();

        ComboBox<?> comboBox = robot.lookup("#userComboBox")
                .queryAs(ComboBox.class);

        robot.interact(() -> {
            assertTrue(comboBox.getItems().isEmpty());
            assertTrue(comboBox.getSelectionModel().isEmpty());
        });
    }

    @Test
    void doesNotCreateUserWhenDialogIsCancelled(FxRobot robot) {
        loadLanding(List.of());

        robot.clickOn("Create User");

        DialogPane dialogPane = robot.lookup(".dialog-pane")
                .queryAs(DialogPane.class);

        Button cancelButton = (Button) dialogPane
                .lookupButton(ButtonType.CANCEL);

        robot.clickOn(cancelButton);

        verify(userService, never()).addUser(any(User.class));
        verify(userService).getAllUsers();

        ComboBox<?> comboBox = robot.lookup("#userComboBox")
                .queryAs(ComboBox.class);

        robot.interact(() -> {
            assertTrue(comboBox.getItems().isEmpty());
            assertTrue(comboBox.getSelectionModel().isEmpty());
        });
    }
}
