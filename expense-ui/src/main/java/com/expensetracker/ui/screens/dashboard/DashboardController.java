package com.expensetracker.ui.screens.dashboard;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import com.expensetracker.models.ExpenseProfile;
import com.expensetracker.models.FinancialTransaction;
import com.expensetracker.models.User;
import com.expensetracker.services.ExpenseProfileService;
import com.expensetracker.services.UserService;
import com.expensetracker.ui.context.UserContext;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Controls the dashboard screen and displays information about
 * the currently selected user and expense profile.
 *
 * <p>
 * Retrieves user and profile details through application services
 * using identifiers maintained by {@link UserContext}.
 */
public final class DashboardController {
    private final UserContext userContext;
    private final UserService userService;
    private final ExpenseProfileService expenseProfileService;
    private final DashboardService dashboardService;

    @FXML
    private Label userLabel;
    @FXML
    private Label profileLabel;
    @FXML
    private Label thisMonthLabel;
    @FXML
    private Label lastMonthLabel;
    @FXML
    private Label thisYearLabel;
    @FXML
    private VBox recentExpensesContainer;

    /**
     * Creates the dashboard controller with its required dependencies.
     *
     * @param userContext           context containing the selected user and profile
     * @param userService           service used to retrieve user information
     * @param expenseProfileService service used to retrieve expense profiles
     * @param dashboardService      service used to retrieve dashboard data
     */
    public DashboardController(UserContext userContext, UserService userService,
            ExpenseProfileService expenseProfileService, DashboardService dashboardService) {
        this.userContext = Objects.requireNonNull(userContext, "userContext must not be null");
        this.userService = Objects.requireNonNull(userService, "userService must not be null");
        this.expenseProfileService = Objects.requireNonNull(expenseProfileService,
                "expenseProfileService must not be null");
        this.dashboardService = Objects.requireNonNull(dashboardService, "dashboardService must not be null");
    }

    @FXML
    private void initialize() {
        Long userId = userContext.getCurrentUserId();
        Long profileId = userContext.getSelectedProfileId();

        if (userId == null) {
            throw new IllegalStateException("No user selected");
        }

        if (profileId == null) {
            throw new IllegalStateException("No expense profile selected");
        }

        User user = userService.getUserById(userId)
                .orElseThrow(() -> new IllegalStateException(
                        "User not found: " + userId));

        ExpenseProfile expenseProfile = expenseProfileService
                .getProfileByUserId(userId)
                .orElseThrow(() -> new IllegalStateException(
                        "Expense profile not found for user: " + userId));

        if (!profileId.equals(expenseProfile.getId())) {
            throw new IllegalStateException(
                    "Selected profile does not match the retrieved profile");
        }

        userLabel.setText("User: " + user.getFirstName());
        profileLabel.setText("Profile: " + expenseProfile.getProfileName());

        DashboardData dashboardData = dashboardService.getDashboardData();
        thisMonthLabel.setText(formatCurrency(dashboardData.thisMonth()));
        lastMonthLabel.setText(formatCurrency(dashboardData.lastMonth()));
        thisYearLabel.setText(formatCurrency(dashboardData.thisYear()));

        displayRecentExpenses(dashboardData.recentTransactions());
    }

    private static String formatCurrency(long amountInCents) {
        BigDecimal amount = BigDecimal.valueOf(amountInCents, 2);

        NumberFormat formatter = NumberFormat.getCurrencyInstance(Locale.US);
        return formatter.format(amount);
    }

    private void displayRecentExpenses(List<FinancialTransaction> transactions) {
        recentExpensesContainer.getChildren().clear();

        if (transactions.isEmpty()) {
            VBox emptyState = new VBox(10);
            emptyState.setAlignment(Pos.CENTER);
            emptyState.setMinHeight(170);
            emptyState.getStyleClass().add("expenses-empty-state");

            Label heading = new Label("No expenses recorded yet");
            heading.getStyleClass().add("empty-state-heading");

            Label description = new Label(
                    "Your latest expenses will appear here.");
            description.getStyleClass().add("empty-state-description");

            emptyState.getChildren().addAll(heading, description);
            recentExpensesContainer.getChildren().add(emptyState);
            return;
        }

        for (FinancialTransaction transaction : transactions) {
            recentExpensesContainer.getChildren().add(
                    createExpenseRow(transaction));
        }
    }

    private HBox createExpenseRow(FinancialTransaction transaction) {
        HBox row = new HBox(16);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("recent-expense-row");

        VBox details = new VBox(5);
        HBox.setHgrow(details, Priority.ALWAYS);

        String description = transaction.getDescription();
        if (description == null || description.isBlank()) {
            description = "Expense";
        }

        Label descriptionLabel = new Label(description);
        descriptionLabel.getStyleClass().add("recent-expense-description");

        Label dateLabel = new Label(
                transaction.getTransactionDate().toString());
        dateLabel.getStyleClass().add("recent-expense-date");

        details.getChildren().addAll(descriptionLabel, dateLabel);

        Label amountLabel = new Label(
                formatCurrency(transaction.getAmount()));
        amountLabel.getStyleClass().add("recent-expense-amount");

        row.getChildren().addAll(details, amountLabel);

        return row;
    }

}
