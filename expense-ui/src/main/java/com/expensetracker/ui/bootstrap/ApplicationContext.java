package com.expensetracker.ui.bootstrap;

import com.expensetracker.services.CategoryService;
import com.expensetracker.services.ExpenseProfileService;
import com.expensetracker.services.FinancialTransactionService;
import com.expensetracker.services.UserService;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * Holds the initialized application services required by the Expense Tracker.
 *
 * <p>
 * Acts as a container for service dependencies created during application
 * bootstrap. Provides access to service interfaces without exposing their
 * concrete implementations or underlying persistence infrastructure.
 *
 * <p>
 * The context is initialized once during application startup and remains
 * immutable throughout the application's lifecycle.
 *
 * <p>
 * User-specific state, such as the currently selected user and expense
 * profile, is managed separately by {@code UserContext}.
 */
@Getter
@RequiredArgsConstructor
public class ApplicationContext {
    @NonNull
    private final UserService userService;
    @NonNull
    private final ExpenseProfileService expenseProfileService;
    @NonNull
    private final CategoryService categoryService;
    @NonNull
    private final FinancialTransactionService financialTransactionService;
}
