package com.expensetracker.ui.screens.expense;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;
import java.util.function.UnaryOperator;

import com.expensetracker.models.Category;
import com.expensetracker.models.Necessity;
import com.expensetracker.models.TransactionType;
import com.expensetracker.persistence.exceptions.PersistenceException;
import com.expensetracker.services.CategoryService;
import com.expensetracker.ui.alerts.AlertService;
import com.expensetracker.ui.converters.CategoryConverter;
import com.expensetracker.ui.converters.NecessityConverter;
import com.expensetracker.ui.navigation.DialogResultProvider;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.stage.Stage;

/**
 * Controls the Add Expense dialog.
 *
 * <p>
 * Manages expense input fields and coordinates user interactions
 * with the expense entry form.
 */
public final class AddExpenseController implements DialogResultProvider {
    private final Clock clock;
    private final CategoryService categoryService;
    private final ExpenseService expenseService;
    private final AlertService alertService;
    private boolean isExpenseSaved;

    @FXML
    private TextField amountField;

    @FXML
    private ComboBox<Category> categoryComboBox;

    @FXML
    private DatePicker transactionDatePicker;

    @FXML
    private ComboBox<Necessity> necessityComboBox;

    @FXML
    private TextArea descriptionArea;

    @FXML
    private Label validationLabel;

    @FXML
    private Button cancelButton;

    @FXML
    private Button saveButton;

    /**
     * Creates the Add Expense controller.
     *
     * @param clock           clock used to determine the current date
     * @param categoryService service used to retrieve expense categories
     * @param expenseService  service responsible for handling addition of expenses
     * @param alertService    service responsible for showing alerts
     */
    public AddExpenseController(Clock clock, CategoryService categoryService, ExpenseService expenseService,
            AlertService alertService) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.categoryService = Objects.requireNonNull(
                categoryService, "categoryService must not be null");
        this.expenseService = Objects.requireNonNull(expenseService, "expenseService must not be null");
        this.alertService = Objects.requireNonNull(alertService, "alertService must not be null");
    }

    /**
     * Initializes the expense entry form.
     */
    @FXML
    private void initialize() {
        configureAmountField();
        necessityComboBox.setItems(
                FXCollections.observableArrayList(Necessity.values()));
        necessityComboBox.setConverter(new NecessityConverter());

        categoryComboBox.setItems(
                FXCollections.observableArrayList(
                        categoryService.getCategoriesByType(TransactionType.EXPENSE)
                                .stream()
                                .filter(Category::isActive)
                                .toList()));

        categoryComboBox.setConverter(new CategoryConverter());

        transactionDatePicker.setValue(LocalDate.now(clock));

        validationLabel.setVisible(false);
        validationLabel.setManaged(false);
    }

    /**
     * Closes the Add Expense dialog without saving any changes.
     */
    @FXML
    private void handleCancel() {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }

    /**
     * Validates and saves the expense entered by the user.
     *
     * <p>
     * Displays a validation message when the supplied input is invalid.
     * Closes the dialog only after the expense is successfully saved.
     */
    @FXML
    private void handleSave() {
        clearValidationError();
        try {
            expenseService.addExpense(
                    amountField.getText(),
                    categoryComboBox.getValue(),
                    transactionDatePicker.getValue(),
                    necessityComboBox.getValue(),
                    descriptionArea.getText());

            Stage stage = (Stage) saveButton.getScene().getWindow();
            isExpenseSaved = true;
            stage.close();

        } catch (IllegalArgumentException exception) {
            showValidationError(exception.getMessage());
        } catch (PersistenceException exception) {
            exception.printStackTrace();
            alertService.showError(
                    "Unable to Save Expense",
                    "Database Error",
                    "Your expense could not be saved. Please try again.");
        }
    }

    /**
     * Configures the amount field to accept nonnegative decimal
     * values with at most two digits after the decimal point.
     */
    private void configureAmountField() {
        UnaryOperator<TextFormatter.Change> filter = change -> {
            String newText = change.getControlNewText();

            if (newText.matches("\\d*(\\.\\d{0,2})?")) {
                return change;
            }

            return null;
        };

        amountField.setTextFormatter(new TextFormatter<>(filter));
    }

    /**
     * Displays a validation error message in the expense dialog.
     *
     * @param message the error message to display
     */
    private void showValidationError(String message) {
        validationLabel.setText(message);
        validationLabel.setVisible(true);
        validationLabel.setManaged(true);
    }

    /**
     * Clears the currently displayed validation error.
     */
    private void clearValidationError() {
        validationLabel.setText("");
        validationLabel.setVisible(false);
        validationLabel.setManaged(false);
    }

    /**
     * Indicates whether an expense was successfully recorded.
     *
     * @return {@code true} if the expense was saved successfully;
     *         {@code false} otherwise
     */
    @Override
    public boolean isSuccessful() {
        return isExpenseSaved;
    }
}
