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
import com.expensetracker.ui.context.UserContext;
import com.expensetracker.ui.navigation.ControllerFactory;
import com.expensetracker.ui.navigation.Dialog;
import com.expensetracker.ui.navigation.DialogResultProvider;
import com.expensetracker.ui.navigation.Screen;
import com.expensetracker.ui.profile.ProfileService;
import com.expensetracker.ui.screens.dashboard.DashboardService;
import com.expensetracker.ui.screens.expense.ExpenseService;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
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

                controllerFactory = new ControllerFactory(applicationContext, userContext, alertService,
                                profileService, dashboardService, expenseService, clock, this::navigateTo,
                                this::showDialog);

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
                        FXMLLoader loader = new FXMLLoader(
                                        Objects.requireNonNull(
                                                        getClass().getResource(screen.getResourcePath()),
                                                        "FXML resource not found: " + screen));

                        loader.setControllerFactory(controllerFactory);

                        Parent root = loader.load();

                        scene.setRoot(root);
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

        public static void main(String[] args) {
                launch(args);
        }
}
