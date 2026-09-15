package application;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.ArrayList;

// Main class 
public class Main extends Application {

    // Required data storage 
    private ArrayList<BankAccount> accounts = new ArrayList<>();
    private ArrayList<Transaction> transactions = new ArrayList<>();

    // Shared UI pieces
    private ComboBox<BankAccount> accountCombo;
    private TextArea statusArea;

  
    public static void main(String[] args) {
        launch(args);   
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("SmartBank");

        Label title = new Label("SmartBank");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #1f3a5f;");

        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getTabs().addAll(
                new Tab("Create Account",      buildCreatePane()),
                new Tab("Deposit / Withdraw",  buildTransactionPane()),
                new Tab("View Accounts",       buildViewPane()),
                new Tab("Transaction History", buildHistoryPane())
        );

        statusArea = new TextArea();
        statusArea.setEditable(false);
        statusArea.setPrefRowCount(5);
        statusArea.setPromptText("Status messages appear here...");

        VBox root = new VBox(10, title, tabs, new Label("Status:"), statusArea);
        root.setPadding(new Insets(15));

        Scene scene = new Scene(root, 640, 660);
        primaryStage.setScene(scene);
        primaryStage.show();
    }


    // TAB 1 — Create a new account (Savings or Credit)
    private Pane buildCreatePane() {
        GridPane grid = new GridPane();
        grid.setPadding(new Insets(20));
        grid.setHgap(10);
        grid.setVgap(10);

        TextField nameField = new TextField();
        nameField.setPromptText("e.g. Jane Doe");

        ComboBox<String> typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll("Savings", "Credit");
        typeCombo.setValue("Savings");

        TextField depositField = new TextField();
        depositField.setPromptText("e.g. 500");

        TextField extraField = new TextField();
        extraField.setPromptText("Minimum balance (Savings) or Credit limit (Credit)");

        Label extraLabel = new Label("Min. balance:");
        typeCombo.valueProperty().addListener((obs, oldV, newV) -> {
            extraLabel.setText("Savings".equals(newV) ? "Min. balance:" : "Credit limit:");
        });

        Button createBtn = new Button("Create Account");
        createBtn.setOnAction(e -> {
            try {
                String name = nameField.getText().trim();
                if (name.isEmpty()) {
                    throw new IllegalArgumentException("Holder name cannot be empty.");
                }

                double initial = Double.parseDouble(depositField.getText().trim());
                if (initial < 0) {
                    throw new IllegalArgumentException("Initial deposit cannot be negative.");
                }

                double extra = Double.parseDouble(extraField.getText().trim());
                if (extra < 0) {
                    throw new IllegalArgumentException("Value cannot be negative.");
                }

                BankAccount acct;
                if ("Savings".equals(typeCombo.getValue())) {
                    if (initial < extra) {
                        throw new IllegalArgumentException(
                                "Initial deposit must meet the minimum balance.");
                    }
                    acct = new SavingsAccount(name, initial, extra);
                } else {
                    acct = new CreditAccount(name, initial, extra);
                }

                accounts.add(acct);
                transactions.add(new Transaction(acct.getAccountNumber(), "OPEN", initial));

                log("✓ Account created: #" + acct.getAccountNumber() + " for " + name);
                refreshCombo();

                nameField.clear();
                depositField.clear();
                extraField.clear();

            } catch (NumberFormatException ex) {
                log("✗ Please enter valid numeric values for deposit and limit.");
            } catch (IllegalArgumentException ex) {
                log("✗ " + ex.getMessage());
            }
        });

        grid.add(new Label("Holder name:"),   0, 0); grid.add(nameField,    1, 0);
        grid.add(new Label("Account type:"),  0, 1); grid.add(typeCombo,    1, 1);
        grid.add(new Label("Initial deposit:"), 0, 2); grid.add(depositField, 1, 2);
        grid.add(extraLabel,                  0, 3); grid.add(extraField,   1, 3);
        grid.add(createBtn,                   1, 4);

        return grid;
    }

    // TAB 2 — Deposit or Withdraw
    private Pane buildTransactionPane() {
        VBox box = new VBox(10);
        box.setPadding(new Insets(20));

        accountCombo = new ComboBox<>();
        accountCombo.setPromptText("Select an account");
        accountCombo.setPrefWidth(420);

        Button refreshBtn = new Button("Refresh accounts");
        refreshBtn.setOnAction(e -> refreshCombo());

        TextField amountField = new TextField();
        amountField.setPromptText("Amount");
        amountField.setPrefWidth(200);

        Button depositBtn  = new Button("Deposit");
        Button withdrawBtn = new Button("Withdraw");

        depositBtn.setOnAction(e -> {
            BankAccount acct = accountCombo.getValue();
            if (acct == null) { log("✗ Please select an account."); return; }
            try {
                double amt = Double.parseDouble(amountField.getText().trim());
                acct.deposit(amt);
                transactions.add(new Transaction(acct.getAccountNumber(), "DEPOSIT", amt));
                log(String.format("✓ Deposited $%.2f to #%d. New balance: $%.2f",
                        amt, acct.getAccountNumber(), acct.getBalance()));
                amountField.clear();
                refreshCombo(); // updates balance shown in the combo label
            } catch (NumberFormatException ex) {
                log("✗ Please enter a valid number.");
            } catch (IllegalArgumentException ex) {
                log("✗ " + ex.getMessage());
            }
        });

        withdrawBtn.setOnAction(e -> {
            BankAccount acct = accountCombo.getValue();
            if (acct == null) { log("✗ Please select an account."); return; }
            try {
                double amt = Double.parseDouble(amountField.getText().trim());
                acct.withdraw(amt);   // polymorphism — calls Savings/Credit override
                transactions.add(new Transaction(acct.getAccountNumber(), "WITHDRAW", amt));
                log(String.format("✓ Withdrew $%.2f from #%d. New balance: $%.2f",
                        amt, acct.getAccountNumber(), acct.getBalance()));
                amountField.clear();
                refreshCombo();
            } catch (NumberFormatException ex) {
                log("✗ Please enter a valid number.");
            } catch (IllegalArgumentException ex) {
                log("✗ " + ex.getMessage());
            } catch (InsufficientBalanceException ex) {
                log("✗ " + ex.getMessage());
            }
        });

        HBox buttons = new HBox(10, depositBtn, withdrawBtn);
        HBox top = new HBox(10, accountCombo, refreshBtn);

        box.getChildren().addAll(
                new Label("Account:"), top,
                new Label("Amount:"), amountField,
                buttons);
        return box;
    }

    // TAB 3 — View all account details
    private Pane buildViewPane() {
        VBox box = new VBox(10);
        box.setPadding(new Insets(20));

        TextArea area = new TextArea();
        area.setEditable(false);
        area.setStyle("-fx-font-family: 'monospace';");

        Button refreshBtn = new Button("Refresh Account List");
        refreshBtn.setOnAction(e -> {
            if (accounts.isEmpty()) {
                area.setText("No accounts yet. Create one in the first tab.");
                return;
            }
            StringBuilder sb = new StringBuilder();
            // Loop as required by the rubric
            for (BankAccount acct : accounts) {
                sb.append(acct.displayAccountDetails()).append("\n");
                sb.append("--------------------------------------\n");
            }
            area.setText(sb.toString());
        });

        box.getChildren().addAll(refreshBtn, area);
        VBox.setVgrow(area, Priority.ALWAYS);
        return box;
    }

    // ==================================================================
    // TAB 4 — Transaction history
    // ==================================================================
    private Pane buildHistoryPane() {
        VBox box = new VBox(10);
        box.setPadding(new Insets(20));

        ListView<String> list = new ListView<>();
        list.setStyle("-fx-font-family: 'monospace';");

        ComboBox<String> filterCombo = new ComboBox<>();
        filterCombo.getItems().addAll("All accounts");
        filterCombo.setValue("All accounts");

        Button refreshBtn = new Button("Refresh History");
        refreshBtn.setOnAction(e -> {
            // Rebuild filter options
            filterCombo.getItems().setAll("All accounts");
            for (BankAccount a : accounts) {
                filterCombo.getItems().add("Account #" + a.getAccountNumber());
            }

            list.getItems().clear();
            String filter = filterCombo.getValue();
            // Loop over all transactions — also required
            for (Transaction t : transactions) {
                if ("All accounts".equals(filter)
                        || filter == null
                        || filter.endsWith(String.valueOf(t.getAccountNumber()))) {
                    list.getItems().add(t.toString());
                }
            }
            if (list.getItems().isEmpty()) {
                list.getItems().add("No transactions to display.");
            }
        });

        HBox top = new HBox(10, new Label("Filter:"), filterCombo, refreshBtn);
        box.getChildren().addAll(top, list);
        VBox.setVgrow(list, Priority.ALWAYS);
        return box;
    }

    // Helpers
    private void refreshCombo() {
        if (accountCombo == null) return;
        BankAccount selected = accountCombo.getValue();
        accountCombo.getItems().setAll(accounts);
        if (selected != null && accounts.contains(selected)) {
            accountCombo.setValue(selected);
        }
    }

    private void log(String message) {
        statusArea.appendText(message + "\n");
    }
}