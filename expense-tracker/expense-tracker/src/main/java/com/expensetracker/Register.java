
package com.expensetracker;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

public class Register {

    public void show(Stage stage) {

        stage.setTitle("Expense Tracker - Registration");

        Label title = new Label("Create Account");

        TextField usernameField = new TextField();
        usernameField.setPromptText("Enter username");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Enter password");

        PasswordField confirmPasswordField = new PasswordField();
        confirmPasswordField.setPromptText("Confirm password");

        Button registerButton = new Button("Register");
        Button loginButton = new Button("Back to Login");

        registerButton.setOnAction(event -> {

            String username = usernameField.getText().trim();
            String password = passwordField.getText();
            String confirmPassword = confirmPasswordField.getText();

            if (username.isEmpty() || password.isEmpty()
                    || confirmPassword.isEmpty()) {

                showAlert("Error", "Please fill in all fields.");
                return;
            }

            if (password.length() < 6) {
                showAlert("Error", "Password must be at least 6 characters.");
                return;
            }

            if (!password.equals(confirmPassword)) {
                showAlert("Error", "Passwords do not match.");
                return;
            }

            try {

                boolean success = AuthService.register(username, password);

                if (success) {

                    showAlert(
                            "Registration Successful",
                            "Your account has been created!"
                    );

                    new Login().show(stage);

                } else {

                    showAlert(
                            "Registration Failed",
                            "Username already exists."
                    );
                }

            } catch (Exception ex) {

                showAlert("Database Error", ex.getMessage());
                ex.printStackTrace();
            }
        });

        loginButton.setOnAction(event ->
                new Login().show(stage)
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

        grid.add(new Label("Confirm Password:"), 0, 3);
        grid.add(confirmPasswordField, 1, 3);

        grid.add(registerButton, 0, 4);
        grid.add(loginButton, 1, 4);

        Scene scene = new Scene(grid, 450, 320);

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
