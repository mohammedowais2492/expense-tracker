package com.expensetracker.ui.navigation;

import java.time.Clock;
import java.util.function.Consumer;
import java.util.function.Function;

import com.expensetracker.ui.alerts.AlertService;
import com.expensetracker.ui.bootstrap.ApplicationContext;
import com.expensetracker.ui.context.UserContext;
import com.expensetracker.ui.profile.ProfileService;
import com.expensetracker.ui.screens.dashboard.DashboardController;
import com.expensetracker.ui.screens.dashboard.DashboardService;
import com.expensetracker.ui.screens.expense.AddExpenseController;
import com.expensetracker.ui.screens.expense.ExpenseService;
import com.expensetracker.ui.screens.landing.LandingController;

import javafx.util.Callback;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * Creates JavaFX controllers with their required application dependencies.
 *
 * <p>
 * Acts as the controller factory for {@link javafx.fxml.FXMLLoader},
 * supplying initialized application services and shared UI context
 * to controllers through constructor injection.
 *
 * <p>
 * Controller construction is centralized here so that individual
 * controllers do not need to create service implementations
 * or access global application state.
 *
 * <p>
 * Only explicitly registered controller types are supported.
 */
@RequiredArgsConstructor
public final class ControllerFactory implements Callback<Class<?>, Object> {
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
    private final Clock clock;
    @NonNull
    private final Consumer<Screen> navigation;
    @NonNull
    private final Function<Dialog, Boolean> dialogHandler;

    @Override
    public Object call(Class<?> controllerClass) {
        if (controllerClass == LandingController.class) {
            return new LandingController(applicationContext.getUserService(), userContext, alertService,
                    profileService, navigation);
        }
        if (controllerClass == DashboardController.class) {
            return new DashboardController(userContext, applicationContext.getUserService(),
                    applicationContext.getExpenseProfileService(), dashboardService, dialogHandler);
        }
        if (controllerClass == AddExpenseController.class) {
            return new AddExpenseController(clock, applicationContext.getCategoryService(), expenseService,
                    alertService);
        }
        throw new IllegalArgumentException("Unsupported controller: " + controllerClass);
    }
}
