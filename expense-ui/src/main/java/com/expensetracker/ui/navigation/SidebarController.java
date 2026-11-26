package com.expensetracker.ui.navigation;

import java.util.function.Consumer;
import java.util.function.Supplier;

import com.expensetracker.ui.alerts.AlertService;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * Controls navigation through the application's shared sidebar.
 *
 * <p>
 * Delegates screen changes to an injected navigation handler and
 * displays an error alert when navigation fails.
 *
 * <p>
 * The sidebar provides navigation between the Dashboard and Transactions
 * screens. Additional navigation options can be enabled as their
 * corresponding features are implemented.
 */
@RequiredArgsConstructor
public final class SidebarController {
    @NonNull
    private final Consumer<Screen> navigation;
    @NonNull
    private final AlertService alertService;
    @NonNull
    private final Supplier<Screen> getActiveScreen;

    @FXML
    private Button dashboardButton;

    @FXML
    private Button transactionsButton;

    @FXML
    private void handleDashboard() {
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
    private void handleTransactions() {
        try {
            navigation.accept(Screen.TRANSACTIONS);
        } catch (RuntimeException exception) {
            exception.printStackTrace();
            alertService.showError(
                    "Navigation Failed",
                    "Unable to open transactions",
                    "An unexpected error occurred while loading the transactions.");
        }
    }

    /**
     * Updates the sidebar button styles to reflect the currently active screen.
     */
    public void refreshActiveScreen() {
        Screen activeScreen = getActiveScreen.get();

        dashboardButton.getStyleClass().remove("sidebar-item-active");
        transactionsButton.getStyleClass().remove("sidebar-item-active");

        if (activeScreen == Screen.DASHBOARD) {
            dashboardButton.getStyleClass().add("sidebar-item-active");
        } else if (activeScreen == Screen.TRANSACTIONS) {
            transactionsButton.getStyleClass().add("sidebar-item-active");
        }
    }
}
