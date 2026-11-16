package com.expensetracker.ui.screens.expense;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

import com.expensetracker.models.Category;
import com.expensetracker.models.FinancialTransaction;
import com.expensetracker.models.Necessity;
import com.expensetracker.models.TransactionType;
import com.expensetracker.services.FinancialTransactionService;
import com.expensetracker.ui.context.UserContext;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * Coordinates expense creation from the desktop UI.
 *
 * <p>
 * Validates expense-entry data, converts user-entered monetary amounts
 * into integer cents, and constructs financial transactions using
 * the currently selected expense profile.
 *
 * <p>
 * Delegates transaction persistence and business-rule enforcement to
 * {@link FinancialTransactionService}. This service contains no JavaFX
 * dependencies and does not manage UI controls or error presentation.
 */
@RequiredArgsConstructor
public final class ExpenseService {
    @NonNull
    private final UserContext userContext;
    @NonNull
    private final FinancialTransactionService financialTransactionService;

    /**
     * Validates and records an expense for the currently selected profile.
     *
     * @param amount          the user-entered dollar amount
     * @param category        the selected expense category
     * @param transactionDate the date of the expense
     * @param necessity       the necessity classification
     * @param description     optional description of the expense
     * @throws IllegalArgumentException if the supplied expense data is invalid
     * @throws IllegalStateException    if no expense profile is selected
     */
    public void addExpense(
            String amount,
            Category category,
            LocalDate transactionDate,
            Necessity necessity,
            String description) {
        Long profileId = userContext.getSelectedProfileId();

        if (profileId == null) {
            throw new IllegalStateException("No expense profile selected");
        }
        validateExpense(category, transactionDate, necessity);
        this.financialTransactionService.createFinancialTransaction(
                FinancialTransaction.builder()
                        .transactionDate(transactionDate)
                        .amount(parseAmountInCents(amount))
                        .categoryId(category.getId())
                        .description(normalizeDescription(description))
                        .necessity(necessity)
                        .profileId(profileId)
                        .type(TransactionType.EXPENSE)
                        .build());
    }

    /**
     * Validates the fields required to create an expense.
     *
     * @param category        the selected category
     * @param transactionDate the transaction date
     * @param necessity       the necessity classification
     * @throws NullPointerException     if any required field is null
     * @throws IllegalArgumentException if the category is not an active
     *                                  expense category
     */
    private static void validateExpense(
            Category category,
            LocalDate transactionDate,
            Necessity necessity) {
        if (category == null) {
            throw new IllegalArgumentException("Please select a category.");
        }

        if (transactionDate == null) {
            throw new IllegalArgumentException("Please select a transaction date.");
        }

        if (necessity == null) {
            throw new IllegalArgumentException("Please select whether the expense was necessary.");
        }
        if (category.getType() != TransactionType.EXPENSE) {
            throw new IllegalArgumentException(
                    "Category must be an expense category");
        }

        if (!category.isActive()) {
            throw new IllegalArgumentException(
                    "Cannot record an expense using an inactive category");
        }
    }

    /**
     * Converts an optional description into its normalized form.
     *
     * @param description the user-entered description
     * @return the trimmed description, or null if absent or blank
     */
    private static String normalizeDescription(String description) {
        if (description == null) {
            return null;
        }

        String normalized = description.strip();
        return normalized.isEmpty() ? null : normalized;
    }

    private static long parseAmountInCents(String input) {
        try {
            if (input == null || input.isBlank()) {
                throw new IllegalArgumentException("Please enter an amount");
            }
            BigDecimal amount = new BigDecimal(input.strip());

            if (amount.signum() <= 0) {
                throw new IllegalArgumentException(
                        "Amount must be greater than zero");
            }

            return amount.movePointRight(2)
                    .longValueExact();
        } catch (NumberFormatException | ArithmeticException exception) {
            throw new IllegalArgumentException(
                    "Enter a valid amount with at most two decimal places",
                    exception);
        }
    }
}
