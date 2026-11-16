package com.expensetracker.ui.screens.landing;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import com.expensetracker.models.User;
import com.expensetracker.services.UserService;
import com.expensetracker.ui.alerts.AlertService;
import com.expensetracker.ui.context.UserContext;
import com.expensetracker.ui.converters.UserStringConverter;
import com.expensetracker.ui.navigation.Screen;
import com.expensetracker.ui.profile.ProfileService;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;

/**
 * Controls the user-selection landing screen.
 *
 * <p>
 * Retrieves existing users through {@link UserService} and displays
 * their names in a JavaFX selection control without exposing
 * database-generated user IDs.
 *
 * <p>
 * Validates user selection and updates the shared
 * {@link UserContext} when the user continues.
 */
public final class LandingController {
    private final UserService userService;
    private final UserContext userContext;
    private final AlertService alertService;
    private final ProfileService profileService;
    private final Consumer<Screen> navigation;

    @FXML
    private ComboBox<User> userComboBox;
    @FXML
    private Label statusLabel;
    @FXML
    private Button continueButton;

    /**
     * Creates the landing controller.
     *
     * @param userService    service used to retrieve users
     * @param userContext    context holding the selected user
     * @param alertService   service used to present alerts
     * @param profileService service responsible for profile management
     * @param navigation     callback used to request screen navigation
     */
    public LandingController(UserService userService, UserContext userContext, AlertService alertService,
            ProfileService profileService, Consumer<Screen> navigation) {
        this.userService = Objects.requireNonNull(userService, "userService must not be null");
        this.userContext = Objects.requireNonNull(userContext, "userContext must not be null");
        this.alertService = Objects.requireNonNull(alertService, "alertService must not be null");
        this.profileService = Objects.requireNonNull(profileService, "profileService must not be null");
        this.navigation = Objects.requireNonNull(navigation, "navigation callback must not be null");
    }

    @FXML
    private void initialize() {
        userComboBox.setConverter(new UserStringConverter());

        refreshUsers();
        if (userComboBox.getItems().isEmpty()) {
            statusLabel.setText(
                    "No users found. Create a user to get started.");
        }
        continueButton.disableProperty().bind(
                userComboBox.valueProperty().isNull());
    }

    @FXML
    private void handleContinue() {
        User selectedUser = userComboBox.getValue();

        if (selectedUser == null) {
            statusLabel.setText("Please select a user.");
            return;
        }

        try {
            userContext.selectUser(selectedUser.getId());

            profileService.selectDefaultProfile();

        } catch (RuntimeException exception) {
            exception.printStackTrace();
            alertService.showError(
                    "Profile Selection Failed",
                    "Unable to initialize expense profile",
                    "An unexpected error occurred while loading your profile.");
            return;
        }

        try {
            navigation.accept(Screen.DASHBOARD);
        } catch (RuntimeException exception) {
            exception.printStackTrace();
            alertService.showError(
                    "Navigation Failed",
                    "Unable to open dashboard",
                    "An unexpected error occurred while loading the dashboard.");
        }
    }

    @FXML
    private void handleCreateUser() {
        try {
            CreateUserDialog dialog = new CreateUserDialog();

            Optional<UserInput> result = dialog.showAndWait();
            if (result.isEmpty()) {
                return;
            }

            UserInput userInput = result.get();
            User persistedUser = userService.addUser(
                    User.builder()
                            .firstName(userInput.firstName())
                            .lastName(userInput.lastName())
                            .build());
            refreshUsers();
            User createdUser = userComboBox.getItems().stream()
                    .filter(user -> user.getId().equals(persistedUser.getId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "Newly created user not found after refresh"));
            userComboBox.setValue(createdUser);
            statusLabel.setText("User created successfully.");
        } catch (RuntimeException exception) {
            exception.printStackTrace();
            alertService.showError(
                    "User Creation Failed",
                    "Unable to create user",
                    "An unexpected error occurred while saving the user");
        }
    }

    private void refreshUsers() {
        userComboBox.getItems().setAll(userService.getAllUsers());
    }
}
