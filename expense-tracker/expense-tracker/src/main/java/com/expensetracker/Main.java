
package com.expensetracker;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileWriter;
import java.sql.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class Main extends Application {

    private Connection conn;
    private int currentUserId;

    private final TableView<Transaction> table = new TableView<>();
    private final ObservableList<Transaction> transactions =
            FXCollections.observableArrayList();

    private final Label incomeLabel = new Label("Income: ₹0.00");
    private final Label expenseLabel = new Label("Expenses: ₹0.00");
    private final Label balanceLabel = new Label("Balance: ₹0.00");

    private final Label monthlyIncomeLabel =
            new Label("Monthly Income: ₹0.00");

    private final Label monthlyExpenseLabel =
            new Label("Monthly Expenses: ₹0.00");

    private final Label monthlyBalanceLabel =
            new Label("Monthly Balance: ₹0.00");

    private final PieChart expenseChart = new PieChart();

    private final ComboBox<Integer> monthBox = new ComboBox<>();
    private final ComboBox<Integer> yearBox = new ComboBox<>();

    private boolean filtered = false;
    private YearMonth selectedMonth = YearMonth.now();

    @Override
    public void start(Stage stage) {

        // Start with login screen
        new Login().show(stage);
    }

    // Called after successful login
    public void showDashboard(Stage stage, int userId) {

        currentUserId = userId;

        connectDatabase();

        if (conn == null) {
            return;
        }

        stage.setTitle("Personal Expense Tracker");

        Button addIncomeButton = new Button("＋ Add Income");
        Button addExpenseButton = new Button("－ Add Expense");
        Button editButton = new Button("✎ Edit Transaction");
        Button deleteButton = new Button("Delete Transaction");
        Button exportButton = new Button("Export CSV");
        Button logoutButton = new Button("Logout");

        addIncomeButton.getStyleClass().add("income-button");
        addExpenseButton.getStyleClass().add("expense-button");
        exportButton.getStyleClass().add("export-button");

        addIncomeButton.setOnAction(e -> showTransactionDialog("Income"));
        addExpenseButton.setOnAction(e -> showTransactionDialog("Expense"));
        editButton.setOnAction(e -> editTransaction());
        deleteButton.setOnAction(e -> deleteTransaction());
        exportButton.setOnAction(e -> exportCSV(stage));

        logoutButton.setOnAction(e -> {
            filtered = false;
            transactions.clear();

            try {
                if (conn != null) {
                    conn.close();
                    conn = null;
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }

            new Login().show(stage);
        });

        // Summary styling
        incomeLabel.getStyleClass().add("summary-label");
        expenseLabel.getStyleClass().add("summary-label");
        balanceLabel.getStyleClass().add("summary-label");

        monthlyIncomeLabel.getStyleClass().add("summary-label");
        monthlyExpenseLabel.getStyleClass().add("summary-label");
        monthlyBalanceLabel.getStyleClass().add("summary-label");

        // Month selector
        monthBox.getItems().clear();
        yearBox.getItems().clear();

        for (int month = 1; month <= 12; month++) {
            monthBox.getItems().add(month);
        }

        int currentYear = LocalDate.now().getYear();

        for (int year = currentYear - 5; year <= currentYear + 1; year++) {
            yearBox.getItems().add(year);
        }

        monthBox.setValue(LocalDate.now().getMonthValue());
        yearBox.setValue(currentYear);

        Button filterButton = new Button("Apply Filter");
        Button showAllButton = new Button("Show All");

        filterButton.setOnAction(e -> {
            filtered = true;

            selectedMonth = YearMonth.of(
                    yearBox.getValue(),
                    monthBox.getValue()
            );

            loadTransactions();
        });

        showAllButton.setOnAction(e -> {
            filtered = false;
            loadTransactions();
        });

        HBox filterBox = new HBox(
                10,
                new Label("Month:"),
                monthBox,
                new Label("Year:"),
                yearBox,
                filterButton,
                showAllButton
        );

        filterBox.setPadding(new Insets(10));

        // Summary layout
        HBox summaryBox = new HBox(
                15,
                incomeLabel,
                expenseLabel,
                balanceLabel
        );

        HBox monthlySummaryBox = new HBox(
                15,
                monthlyIncomeLabel,
                monthlyExpenseLabel,
                monthlyBalanceLabel
        );

        summaryBox.setPadding(new Insets(10));
        monthlySummaryBox.setPadding(new Insets(10));

        // Table columns
        TableColumn<Transaction, Integer> idColumn =
                new TableColumn<>("ID");

        idColumn.setCellValueFactory(
                data -> data.getValue().idProperty().asObject()
        );

        TableColumn<Transaction, String> typeColumn =
                new TableColumn<>("Type");

        typeColumn.setCellValueFactory(
                data -> data.getValue().typeProperty()
        );

        TableColumn<Transaction, String> categoryColumn =
                new TableColumn<>("Category");

        categoryColumn.setCellValueFactory(
                data -> data.getValue().categoryProperty()
        );

        TableColumn<Transaction, String> descriptionColumn =
                new TableColumn<>("Description");

        descriptionColumn.setCellValueFactory(
                data -> data.getValue().descriptionProperty()
        );

        TableColumn<Transaction, Double> amountColumn =
                new TableColumn<>("Amount");

        amountColumn.setCellValueFactory(
                data -> data.getValue().amountProperty().asObject()
        );

        TableColumn<Transaction, String> dateColumn =
                new TableColumn<>("Date");

        dateColumn.setCellValueFactory(
                data -> data.getValue().dateProperty()
        );

        table.getColumns().clear();

        table.getColumns().addAll(
                idColumn,
                typeColumn,
                categoryColumn,
                descriptionColumn,
                amountColumn,
                dateColumn
        );

        table.setItems(transactions);
        table.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY
        );

        // Pie chart
        expenseChart.setTitle("Expenses by Category");
        expenseChart.setPrefSize(450, 400);

        // Buttons
        HBox buttonBox = new HBox(
                10,
                addIncomeButton,
                addExpenseButton,
                editButton,
                deleteButton,
                exportButton,
                logoutButton
        );

        buttonBox.setPadding(new Insets(10));

        // Left dashboard
        VBox leftBox = new VBox(
                10,
                summaryBox,
                monthlySummaryBox,
                filterBox,
                table,
                buttonBox
        );

        leftBox.setPadding(new Insets(15));

        VBox.setVgrow(table, Priority.ALWAYS);
        HBox.setHgrow(leftBox, Priority.ALWAYS);

        // Right dashboard
        VBox rightBox = new VBox(expenseChart);
        rightBox.setPadding(new Insets(20));

        HBox mainContent = new HBox(
                15,
                leftBox,
                rightBox
        );

        VBox root = new VBox(mainContent);

        Scene scene = new Scene(root, 1400, 850);

        // Load CSS if available
        var css = getClass().getResource("/style.css");

        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        }

        stage.setScene(scene);
        stage.show();

        loadTransactions();
    }

    // DATABASE CONNECTION
    private void connectDatabase() {

        try {
            conn = DBConnection.getConnection();

            System.out.println("Database connected!");

        } catch (SQLException e) {
            showAlert("Database Error", e.getMessage());
            e.printStackTrace();
        }
    }

    // ADD TRANSACTION
    private void showTransactionDialog(String type) {

        Dialog<ButtonType> dialog = new Dialog<>();

        dialog.setTitle("Add " + type);

        ButtonType saveButton =
                new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);

        dialog.getDialogPane().getButtonTypes().addAll(
                saveButton,
                ButtonType.CANCEL
        );

        TextField categoryField = new TextField();
        TextField descriptionField = new TextField();
        TextField amountField = new TextField();

        DatePicker datePicker = new DatePicker(LocalDate.now());

        GridPane grid = new GridPane();

        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        grid.add(new Label("Category:"), 0, 0);
        grid.add(categoryField, 1, 0);

        grid.add(new Label("Description:"), 0, 1);
        grid.add(descriptionField, 1, 1);

        grid.add(new Label("Amount:"), 0, 2);
        grid.add(amountField, 1, 2);

        grid.add(new Label("Date:"), 0, 3);
        grid.add(datePicker, 1, 3);

        dialog.getDialogPane().setContent(grid);

        Optional<ButtonType> result = dialog.showAndWait();

        if (result.isPresent() && result.get() == saveButton) {

            try {

                String category = categoryField.getText().trim();
                String description = descriptionField.getText().trim();

                double amount = Double.parseDouble(amountField.getText());

                LocalDate date = datePicker.getValue();

                if (category.isEmpty() || amount <= 0 || date == null) {
                    showAlert(
                            "Invalid Input",
                            "Enter valid transaction details."
                    );
                    return;
                }

                String sql =
                        "INSERT INTO transactions " +
                        "(user_id, type, category, description, amount, transaction_date) " +
                        "VALUES (?, ?, ?, ?, ?, ?)";

                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

                    pstmt.setInt(1, currentUserId);
                    pstmt.setString(2, type);
                    pstmt.setString(3, category);
                    pstmt.setString(4, description);
                    pstmt.setDouble(5, amount);
                    pstmt.setDate(6, Date.valueOf(date));

                    pstmt.executeUpdate();
                }

                loadTransactions();

            } catch (Exception e) {
                showAlert("Input Error", e.getMessage());
            }
        }
    }

    // DATE FILTER
    private String getDateCondition() {

        if (!filtered) {
            return "";
        }

        LocalDate startDate = selectedMonth.atDay(1);
        LocalDate endDate = selectedMonth.plusMonths(1).atDay(1);

        return " AND transaction_date >= '" + startDate +
                "' AND transaction_date < '" + endDate + "'";
    }

    // LOAD TRANSACTIONS FOR CURRENT USER ONLY
    private void loadTransactions() {

        transactions.clear();

        if (conn == null) {
            return;
        }

        String sql =
                "SELECT id, type, category, description, amount, transaction_date " +
                "FROM transactions WHERE user_id = ? " +
                getDateCondition() +
                " ORDER BY transaction_date DESC";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, currentUserId);

            try (ResultSet rs = pstmt.executeQuery()) {

                while (rs.next()) {

                    transactions.add(new Transaction(
                            rs.getInt("id"),
                            rs.getString("type"),
                            rs.getString("category"),
                            rs.getString("description"),
                            rs.getDouble("amount"),
                            rs.getDate("transaction_date").toString()
                    ));
                }
            }

            updateSummary();
            updateMonthlySummary();
            updateExpenseChart();

        } catch (SQLException e) {
            showAlert("Database Error", e.getMessage());
            e.printStackTrace();
        }
    }

    // SUMMARY
    private void updateSummary() {

        double income = 0;
        double expenses = 0;

        for (Transaction t : transactions) {

            if (t.getType().equalsIgnoreCase("Income")) {
                income += t.getAmount();
            } else {
                expenses += t.getAmount();
            }
        }

        incomeLabel.setText(String.format("Income: ₹%.2f", income));
        expenseLabel.setText(String.format("Expenses: ₹%.2f", expenses));
        balanceLabel.setText(
                String.format("Balance: ₹%.2f", income - expenses)
        );
    }

    // MONTHLY SUMMARY
    private void updateMonthlySummary() {

        double income = 0;
        double expenses = 0;

        for (Transaction t : transactions) {

            if (t.getType().equalsIgnoreCase("Income")) {
                income += t.getAmount();
            } else {
                expenses += t.getAmount();
            }
        }

        monthlyIncomeLabel.setText(
                String.format("Monthly Income: ₹%.2f", income)
        );

        monthlyExpenseLabel.setText(
                String.format("Monthly Expenses: ₹%.2f", expenses)
        );

        monthlyBalanceLabel.setText(
                String.format("Monthly Balance: ₹%.2f", income - expenses)
        );
    }

    // PIE CHART
    private void updateExpenseChart() {

        expenseChart.getData().clear();

        Map<String, Double> categoryTotals = new HashMap<>();

        for (Transaction t : transactions) {

            if (t.getType().equalsIgnoreCase("Expense")) {

                categoryTotals.put(
                        t.getCategory(),
                        categoryTotals.getOrDefault(t.getCategory(), 0.0)
                                + t.getAmount()
                );
            }
        }

        for (Map.Entry<String, Double> entry : categoryTotals.entrySet()) {

            expenseChart.getData().add(
                    new PieChart.Data(
                            entry.getKey(),
                            entry.getValue()
                    )
            );
        }
    }

    // EDIT TRANSACTION
    private void editTransaction() {

        Transaction selected =
                table.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showAlert("Selection Required", "Select a transaction first.");
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();

        dialog.setTitle("Edit Transaction");

        ButtonType updateButton =
                new ButtonType("Update", ButtonBar.ButtonData.OK_DONE);

        dialog.getDialogPane().getButtonTypes().addAll(
                updateButton,
                ButtonType.CANCEL
        );

        TextField categoryField =
                new TextField(selected.getCategory());

        TextField descriptionField =
                new TextField(selected.getDescription());

        TextField amountField =
                new TextField(String.valueOf(selected.getAmount()));

        DatePicker datePicker =
                new DatePicker(LocalDate.parse(selected.getDate()));

        GridPane grid = new GridPane();

        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        grid.add(new Label("Category:"), 0, 0);
        grid.add(categoryField, 1, 0);

        grid.add(new Label("Description:"), 0, 1);
        grid.add(descriptionField, 1, 1);

        grid.add(new Label("Amount:"), 0, 2);
        grid.add(amountField, 1, 2);

        grid.add(new Label("Date:"), 0, 3);
        grid.add(datePicker, 1, 3);

        dialog.getDialogPane().setContent(grid);

        Optional<ButtonType> result = dialog.showAndWait();

        if (result.isPresent() && result.get() == updateButton) {

            try {

                double amount = Double.parseDouble(amountField.getText());

                if (amount <= 0) {
                    showAlert("Invalid Amount", "Amount must be positive.");
                    return;
                }

                String sql =
                        "UPDATE transactions SET category=?, description=?, " +
                        "amount=?, transaction_date=? " +
                        "WHERE id=? AND user_id=?";

                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

                    pstmt.setString(1, categoryField.getText().trim());
                    pstmt.setString(2, descriptionField.getText().trim());
                    pstmt.setDouble(3, amount);
                    pstmt.setDate(4, Date.valueOf(datePicker.getValue()));
                    pstmt.setInt(5, selected.getId());
                    pstmt.setInt(6, currentUserId);

                    pstmt.executeUpdate();
                }

                loadTransactions();

            } catch (Exception e) {
                showAlert("Update Error", e.getMessage());
            }
        }
    }

    // DELETE TRANSACTION
    private void deleteTransaction() {

        Transaction selected =
                table.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showAlert("Selection Required", "Select a transaction first.");
            return;
        }

        Alert confirmation =
                new Alert(Alert.AlertType.CONFIRMATION);

        confirmation.setTitle("Delete Transaction");
        confirmation.setHeaderText("Delete this transaction?");
        confirmation.setContentText(selected.getDescription());

        Optional<ButtonType> result = confirmation.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {

            try {

                String sql =
                        "DELETE FROM transactions WHERE id=? AND user_id=?";

                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

                    pstmt.setInt(1, selected.getId());
                    pstmt.setInt(2, currentUserId);

                    pstmt.executeUpdate();
                }

                loadTransactions();

            } catch (SQLException e) {
                showAlert("Delete Error", e.getMessage());
            }
        }
    }

    // CSV EXPORT - CURRENT USER ONLY
    private void exportCSV(Stage stage) {

        FileChooser fileChooser = new FileChooser();

        fileChooser.setTitle("Export Transactions");
        fileChooser.setInitialFileName("transactions.csv");

        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV Files", "*.csv")
        );

        File file = fileChooser.showSaveDialog(stage);

        if (file == null) {
            return;
        }

        String sql =
                "SELECT type, category, description, amount, transaction_date " +
                "FROM transactions WHERE user_id = ? " +
                getDateCondition() +
                " ORDER BY transaction_date DESC";

        try (
                PreparedStatement pstmt = conn.prepareStatement(sql);
                FileWriter writer = new FileWriter(file)
        ) {

            pstmt.setInt(1, currentUserId);

            try (ResultSet rs = pstmt.executeQuery()) {

                // No transaction ID included
                writer.write("Type,Category,Description,Amount,Date\n");

                while (rs.next()) {

                    writer.write(
                            escapeCSV(rs.getString("type")) + "," +
                            escapeCSV(rs.getString("category")) + "," +
                            escapeCSV(rs.getString("description")) + "," +
                            rs.getDouble("amount") + "," +
                            escapeCSV(
                                    rs.getDate("transaction_date").toString()
                            ) +
                            "\n"
                    );
                }
            }

            showAlert(
                    "Export Successful",
                    "CSV saved at:\n" + file.getAbsolutePath()
            );

        } catch (Exception e) {
            showAlert("Export Error", e.getMessage());
        }
    }

    private String escapeCSV(String value) {

        if (value == null) {
            return "";
        }

        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    // ALERT
    private void showAlert(String title, String message) {

        Alert alert = new Alert(Alert.AlertType.INFORMATION);

        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        alert.showAndWait();
    }

    @Override
    public void stop() {

        try {
            if (conn != null) {
                conn.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }

    // TRANSACTION MODEL
    public static class Transaction {

        private final javafx.beans.property.IntegerProperty id;
        private final javafx.beans.property.StringProperty type;
        private final javafx.beans.property.StringProperty category;
        private final javafx.beans.property.StringProperty description;
        private final javafx.beans.property.DoubleProperty amount;
        private final javafx.beans.property.StringProperty date;

        public Transaction(
                int id,
                String type,
                String category,
                String description,
                double amount,
                String date
        ) {

            this.id =
                    new javafx.beans.property.SimpleIntegerProperty(id);

            this.type =
                    new javafx.beans.property.SimpleStringProperty(type);

            this.category =
                    new javafx.beans.property.SimpleStringProperty(category);

            this.description =
                    new javafx.beans.property.SimpleStringProperty(description);

            this.amount =
                    new javafx.beans.property.SimpleDoubleProperty(amount);

            this.date =
                    new javafx.beans.property.SimpleStringProperty(date);
        }

        public int getId() {
            return id.get();
        }

        public String getType() {
            return type.get();
        }

        public String getCategory() {
            return category.get();
        }

        public String getDescription() {
            return description.get();
        }

        public double getAmount() {
            return amount.get();
        }

        public String getDate() {
            return date.get();
        }

        public javafx.beans.property.IntegerProperty idProperty() {
            return id;
        }

        public javafx.beans.property.StringProperty typeProperty() {
            return type;
        }

        public javafx.beans.property.StringProperty categoryProperty() {
            return category;
        }

        public javafx.beans.property.StringProperty descriptionProperty() {
            return description;
        }

        public javafx.beans.property.DoubleProperty amountProperty() {
            return amount;
        }

        public javafx.beans.property.StringProperty dateProperty() {
            return date;
        }
    }
}
