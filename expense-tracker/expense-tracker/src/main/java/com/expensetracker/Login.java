
package com.expensetracker;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

public class Login {

    public void show(Stage stage) {

        stage.setTitle("Expense Tracker - Login");

        Label title = new Label("Welcome Back");

        TextField usernameField = new TextField();
        usernameField.setPromptText("Enter username");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Enter password");

        Button loginButton = new Button("Login");
        Button registerButton = new Button("Create Account");

        loginButton.setOnAction(event -> {

            String username = usernameField.getText().trim();
            String password = passwordField.getText();

            if (username.isEmpty() || password.isEmpty()) {

                showAlert("Error", "Please enter username and password.");
                return;
            }

            try {

                int userId = AuthService.login(username, password);

                if (userId != -1) {

                    showAlert(
                            "Login Successful",
                            "Welcome, " + username + "!"
                    );

                    Main main = new Main();

                    main.showDashboard(stage, userId);

                } else {

                    showAlert(
                            "Login Failed",
                            "Invalid username or password."
                    );
                }

            } catch (Exception ex) {

                showAlert("Database Error", ex.getMessage());
                ex.printStackTrace();
            }
        });

        registerButton.setOnAction(event ->
                new Register().show(stage)
        );

        GridPane grid = new GridPane();

        grid.setPadding(new Insets(30));
        grid.setHgap(10);
        grid.setVgap(15);

        grid.add(title, 0, 0, 2, 1);

        grid.add(new Label("Username:"), 0, 1);
        grid.add(usernameField, 1, 1);

        grid.add(new Label("Password:"), 0, 2);
        grid.add(passwordField, 1, 2);

        grid.add(loginButton, 0, 3);
        grid.add(registerButton, 1, 3);

        Scene scene = new Scene(grid, 400, 250);

        stage.setScene(scene);
        stage.show();
    }

    private void showAlert(String title, String message) {

        Alert alert = new Alert(Alert.AlertType.INFORMATION);

        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        alert.showAndWait();
    }
}
