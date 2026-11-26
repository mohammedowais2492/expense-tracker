package com.expensetracker.ui;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.time.Clock;
import java.util.Objects;

import com.expensetracker.ui.alerts.AlertService;
import com.expensetracker.ui.alerts.JavaFxAlertService;
import com.expensetracker.ui.bootstrap.ApplicationBootstrap;
import com.expensetracker.ui.bootstrap.ApplicationContext;
import com.expensetracker.ui.bootstrap.UiServices;
import com.expensetracker.ui.categories.CategoryLookupService;
import com.expensetracker.ui.context.UserContext;
import com.expensetracker.ui.navigation.ControllerFactory;
import com.expensetracker.ui.navigation.Dialog;
import com.expensetracker.ui.navigation.DialogResultProvider;
import com.expensetracker.ui.navigation.Screen;
import com.expensetracker.ui.navigation.SidebarController;
import com.expensetracker.ui.profile.ProfileService;
import com.expensetracker.ui.screens.dashboard.DashboardService;
import com.expensetracker.ui.screens.expense.ExpenseService;
import com.expensetracker.ui.screens.transactions.TransactionHistoryService;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * Main entry point for the Expense Tracker desktop application.
 *
 * <p>
 * Initializes the JavaFX runtime and configures the
 * application's primary window.
 */
public final class ExpenseTrackerApplication extends Application {
        private Scene scene;
        private ControllerFactory controllerFactory;
        private Stage primaryStage;
        private Screen activeScreen;

        @Override
        public void start(Stage primaryStage) throws IOException {
                this.primaryStage = Objects.requireNonNull(
                                primaryStage, "primaryStage must not be null");
                Path databasePath = Path.of(
                                System.getProperty("user.home"),
                                "expense-tracker.db");

                ApplicationContext applicationContext = ApplicationBootstrap.initialize(databasePath);

                UserContext userContext = new UserContext();

                AlertService alertService = new JavaFxAlertService(primaryStage);

                ProfileService profileService = new ProfileService(applicationContext.getExpenseProfileService(),
                                userContext);

                Clock clock = Clock.systemDefaultZone();

                DashboardService dashboardService = new DashboardService(userContext,
                                applicationContext.getFinancialTransactionService(), clock);

                ExpenseService expenseService = new ExpenseService(userContext,
                                applicationContext.getFinancialTransactionService());

                TransactionHistoryService transactionHistoryService = new TransactionHistoryService(userContext,
                                applicationContext.getFinancialTransactionService());

                CategoryLookupService categoryLookupService = new CategoryLookupService(
                                applicationContext.getCategoryService());

                categoryLookupService.refresh();

                UiServices uiServices = new UiServices(
                                applicationContext,
                                userContext,
                                alertService,
                                profileService,
                                dashboardService,
                                expenseService,
                                transactionHistoryService,
                                categoryLookupService,
                                clock);

                controllerFactory = new ControllerFactory(
                                uiServices,
                                this::navigateTo,
                                this::showDialog,
                                this::getActiveScreen);

                scene = new Scene(new StackPane(), 1000, 650);

                scene.getStylesheets().add(
                                Objects.requireNonNull(
                                                getClass().getResource(
                                                                "/com/expensetracker/ui/styles/application.css"))
                                                .toExternalForm());

                primaryStage.setTitle("Expense Tracker");
                primaryStage.setScene(scene);
                primaryStage.setMinWidth(800);
                primaryStage.setMinHeight(550);

                navigateTo(Screen.LANDING);

                primaryStage.show();
        }

        private void navigateTo(Screen screen) {
                Objects.requireNonNull(screen, "screen must not be null");

                try {
                        FXMLLoader contentLoader = new FXMLLoader(
                                        Objects.requireNonNull(
                                                        getClass().getResource(screen.getResourcePath()),
                                                        "FXML resource not found: " + screen));

                        contentLoader.setControllerFactory(controllerFactory);

                        Parent content = contentLoader.load();

                        if (screen == Screen.LANDING) {
                                scene.setRoot(content);
                                activeScreen = screen;
                                return;
                        }

                        if (screen == Screen.DASHBOARD || screen == Screen.TRANSACTIONS) {
                                FXMLLoader shellLoader = new FXMLLoader(
                                                Objects.requireNonNull(
                                                                getClass().getResource(
                                                                                "/com/expensetracker/ui/layout/app-shell.fxml"),
                                                                "Application shell FXML not found"));

                                shellLoader.setControllerFactory(controllerFactory);

                                BorderPane shell = shellLoader.load();
                                shell.setCenter(content);

                                SidebarController sidebarController = (SidebarController) shellLoader.getNamespace()
                                                .get("sidebarController");

                                scene.setRoot(shell);
                                activeScreen = screen;

                                sidebarController.refreshActiveScreen();
                                return;
                        }

                        throw new IllegalArgumentException(
                                        "Unsupported screen: " + screen);

                } catch (IOException exception) {
                        throw new UncheckedIOException(
                                        "Unable to load screen: " + screen,
                                        exception);
                }
        }

        private boolean showDialog(Dialog dialog) {
                try {
                        FXMLLoader loader = new FXMLLoader(
                                        getClass().getResource(dialog.getFxmlPath()));

                        loader.setControllerFactory(controllerFactory);

                        Parent root = loader.load();

                        Stage dialogStage = new Stage();
                        dialogStage.initOwner(primaryStage);
                        dialogStage.initModality(Modality.WINDOW_MODAL);

                        Scene dialogScene = new Scene(root);
                        dialogScene.getStylesheets().addAll(
                                        scene.getStylesheets());

                        dialogStage.setScene(dialogScene);
                        dialogStage.setResizable(false);
                        dialogStage.showAndWait();

                        Object controller = loader.getController();

                        if (controller instanceof DialogResultProvider resultProvider) {
                                return resultProvider.isSuccessful();
                        }

                        return false;

                } catch (IOException exception) {
                        throw new UncheckedIOException(
                                        "Unable to load dialog: " + dialog, exception);
                }
        }

        private Screen getActiveScreen() {
                return activeScreen;
        }

        public static void main(String[] args) {
                launch(args);
        }
}
