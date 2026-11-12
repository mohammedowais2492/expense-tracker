package com.expensetracker.ui;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Main entry point for the Expense Tracker desktop application.
 *
 * <p>
 * Initializes the JavaFX runtime and configures the
 * application's primary window.
 */
public final class ExpenseTrackerApplication extends Application {
    @Override
    public void start(Stage primaryStage) {
        Label heading = new Label("ExpenseTracker");

        TextField nameField = new TextField();
        nameField.setPromptText("Enter your name");

        Button greetButton = new Button("Greetings");

        Label resultLabel = new Label();

        greetButton.setOnAction(event -> {
            String name = nameField.getText();
            if (name.isBlank()) {
                resultLabel.setText("Please enter your name.");
            } else {
                resultLabel.setText("Hello, " + name.strip() + "!");
            }
        });

        Label statusLabel = new Label("Ready");

        VBox form = new VBox(15, nameField, greetButton, resultLabel);
        form.setAlignment(Pos.CENTER);

        BorderPane root = new BorderPane();
        root.setTop(heading);
        root.setCenter(form);
        root.setBottom(statusLabel);

        BorderPane.setMargin(heading, new Insets(20));
        BorderPane.setAlignment(heading, Pos.CENTER);
        BorderPane.setMargin(statusLabel, new Insets(10));
        BorderPane.setAlignment(statusLabel, Pos.BOTTOM_LEFT);

        // root.getChildren().addAll(
        // heading,
        // nameField,
        // greetButton,
        // resultLabel);

        Scene scene = new Scene(root, 800, 500);

        primaryStage.setTitle("ExpenseTracker");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
