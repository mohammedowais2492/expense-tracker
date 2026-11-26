package com.expensetracker.ui.navigation;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import com.expensetracker.ui.bootstrap.UiServices;
import com.expensetracker.ui.screens.dashboard.DashboardController;
import com.expensetracker.ui.screens.expense.AddExpenseController;
import com.expensetracker.ui.screens.landing.LandingController;
import com.expensetracker.ui.screens.transactions.TransactionsController;

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
    private final UiServices uiServices;
    @NonNull
    private final Consumer<Screen> navigation;
    @NonNull
    private final Function<Dialog, Boolean> dialogHandler;
    @NonNull
    private final Supplier<Screen> getActiveScreen;

    @Override
    public Object call(Class<?> controllerClass) {
        if (controllerClass == LandingController.class) {
            return new LandingController(uiServices.getApplicationContext().getUserService(),
                    uiServices.getUserContext(), uiServices.getAlertService(),
                    uiServices.getProfileService(), navigation);
        }
        if (controllerClass == DashboardController.class) {
            return new DashboardController(uiServices.getUserContext(),
                    uiServices.getApplicationContext().getUserService(),
                    uiServices.getApplicationContext().getExpenseProfileService(), uiServices.getDashboardService(),
                    dialogHandler);
        }
        if (controllerClass == AddExpenseController.class) {
            return new AddExpenseController(uiServices.getClock(),
                    uiServices.getApplicationContext().getCategoryService(), uiServices.getExpenseService(),
                    uiServices.getAlertService());
        }
        if (controllerClass == TransactionsController.class) {
            return new TransactionsController(uiServices.getTransactionHistoryService(),
                    uiServices.getCategoryLookupService());
        }
        if (controllerClass == SidebarController.class) {
            return new SidebarController(navigation, uiServices.getAlertService(), getActiveScreen);
        }
        throw new IllegalArgumentException("Unsupported controller: " + controllerClass);
    }
}
