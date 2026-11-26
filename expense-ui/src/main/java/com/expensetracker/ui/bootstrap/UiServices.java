package com.expensetracker.ui.bootstrap;

import java.time.Clock;

import com.expensetracker.ui.alerts.AlertService;
import com.expensetracker.ui.categories.CategoryLookupService;
import com.expensetracker.ui.context.UserContext;
import com.expensetracker.ui.profile.ProfileService;
import com.expensetracker.ui.screens.dashboard.DashboardService;
import com.expensetracker.ui.screens.expense.ExpenseService;
import com.expensetracker.ui.screens.transactions.TransactionHistoryService;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * Holds the shared dependencies required to construct JavaFX controllers.
 *
 * <p>
 * Dependencies are created by the application composition root and
 * supplied through this immutable holder to the controller factory.
 * The factory then injects only the dependencies required by each
 * individual controller.
 *
 * <p>
 * This class does not construct, manage, or resolve dependencies
 * dynamically. It provides type-safe access to existing application
 * services and shared UI infrastructure.
 */
@Getter
@RequiredArgsConstructor
public final class UiServices {

    @NonNull
    private final ApplicationContext applicationContext;

    @NonNull
    private final UserContext userContext;

    @NonNull
    private final AlertService alertService;

    @NonNull
    private final ProfileService profileService;

    @NonNull
    private final DashboardService dashboardService;

    @NonNull
    private final ExpenseService expenseService;

    @NonNull
    private final TransactionHistoryService transactionHistoryService;

    @NonNull
    private final CategoryLookupService categoryLookupService;

    @NonNull
    private final Clock clock;
}
