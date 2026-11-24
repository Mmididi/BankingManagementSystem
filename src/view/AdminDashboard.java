package view;

import controller.AccountController;
import controller.AccountController.AccountSummary;
import controller.AdminController;
import controller.AdminController.UserSummary;
import controller.LoginController;
import controller.TransactionController;
import controller.TransactionController.TransactionSummary;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.sql.SQLException;
import java.util.List;

public final class AdminDashboard {

    private final AdminController admin;
    private final AccountController accounts;
    private final TransactionController transactions;

    private final BorderPane root = new BorderPane();
    private final TableView<UserSummary> users =
            DashboardStyle.table("No users recorded.");
    private final TableView<AccountSummary> accountRecords =
            DashboardStyle.table("No accounts recorded.");
    private final TableView<TransactionSummary> ledger =
            DashboardStyle.table("No transactions recorded.");

    private final Label activeUsers = new Label("0");
    private final Label accountCount = new Label("0");
    private final Label status = new Label();

    public AdminDashboard(Main main, LoginController login,
                          AdminController admin, AccountController accounts,
                          TransactionController transactions) {
        this.admin = admin;
        this.accounts = accounts;
        this.transactions = transactions;

        HBox header = DashboardStyle.header(
                "Administrator workspace", login.getCurrentUsername(), main::showLogin);
        header.setPadding(new Insets(12));
        root.setTop(header);

        configureTables();

        TextField username = new TextField();
        username.setPromptText("New teller username");

        PasswordField password = new PasswordField();
        password.setPromptText("Password (8+ characters)");

        Button createTeller = button("Create teller", () -> {
            admin.createTeller(username.getText(), password.getText());
            username.clear();
            password.clear();
            refresh();
            status.setText("Teller created.");
        });

        Button resetPassword = button("Reset selected password", () -> {
            if (!admin.resetUserPassword(selectedUser().id(), password.getText())) {
                throw new IllegalStateException("User could not be updated.");
            }

            password.clear();
            status.setText("Password updated.");
        });

        FlowPane credentials = new FlowPane(
                8, 8, username, password, createTeller, resetPassword);

        FlowPane actions = new FlowPane(8, 8,
                button("Activate", () -> setActive(true)),
                button("Deactivate", () -> setActive(false)),
                button("Refresh records", this::refresh)
        );

        Button interest = button("Run monthly interest", () -> {
            Alert confirm = new Alert(
                    Alert.AlertType.CONFIRMATION,
                    "Credit eligible accounts for the current month now?",
                    ButtonType.YES,
                    ButtonType.NO
            );

            if (confirm.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
                int count = transactions.applyMonthlyInterest();
                refresh();
                status.setText("Accounts credited: " + count);
            }
        });

        VBox active = DashboardStyle.card("Active users", activeUsers);
        VBox total = DashboardStyle.card("Bank accounts", accountCount);
        VBox monthly = DashboardStyle.card("Monthly processing", interest);

        HBox summary = new HBox(20, active, total, monthly);

        Label title = new Label("Bank administration");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
        status.setWrapText(true);

        VBox body = new VBox(14,
                title,
                summary,
                status,
                DashboardStyle.card("Manage user access", users, actions, credentials),
                DashboardStyle.card("Account register", accountRecords),
                DashboardStyle.card("Bank-wide ledger", ledger)
        );

        body.setPadding(new Insets(16));
        root.setCenter(DashboardStyle.scroll(body));
        run(this::refresh);
    }

    private void configureTables() {
        users.setId("admin-user-table");
        users.getColumns().setAll(List.of(
                DashboardStyle.column("Username", UserSummary::username),
                DashboardStyle.column("Role", UserSummary::role),
                DashboardStyle.column("Status", user -> user.active() ? "Active" : "Inactive")
        ));
        users.setPrefHeight(240);

        accountRecords.getColumns().setAll(List.of(
                DashboardStyle.column("Account", AccountSummary::accountNumber),
                DashboardStyle.column("Type", AccountSummary::accountTypeName),
                DashboardStyle.column("Branch", AccountSummary::branch),
                DashboardStyle.column("Balance", account -> DashboardStyle.bwp(account.balance()))
        ));
        accountRecords.setPrefHeight(235);

        ledger.setId("admin-ledger");
        ledger.getColumns().setAll(List.of(
                DashboardStyle.column("Date and time",
                        row -> row.transactionDate().replace('T', ' ')),
                DashboardStyle.column("Account", TransactionSummary::accountNumber),
                DashboardStyle.column("Activity", TransactionSummary::transactionType),
                DashboardStyle.column("Amount", row -> {
                    String sign = "WITHDRAWAL".equals(row.transactionType()) ? "−" : "+";
                    return sign + DashboardStyle.bwp(row.amount());
                }),
                DashboardStyle.column("Balance after",
                        row -> DashboardStyle.bwp(row.balanceAfter()))
        ));
        ledger.setPrefHeight(240);
    }

    private UserSummary selectedUser() {
        UserSummary user = users.getSelectionModel().getSelectedItem();

        if (user == null) {
            throw new IllegalArgumentException("Select a user first.");
        }

        return user;
    }

    private void setActive(boolean enabled) throws SQLException {
        String userId = selectedUser().id();
        boolean changed = enabled ? admin.activateUser(userId) : admin.deactivateUser(userId);

        if (!changed) {
            throw new IllegalStateException("User could not be updated.");
        }

        refresh();
        status.setText(enabled ? "User activated." : "User deactivated.");
    }

    private void refresh() throws SQLException {
        UserSummary previous = users.getSelectionModel().getSelectedItem();
        List<UserSummary> savedUsers = admin.getAllUsers();
        users.getItems().setAll(savedUsers);

        if (previous != null) {
            savedUsers.stream()
                    .filter(user -> user.id().equals(previous.id()))
                    .findFirst()
                    .ifPresent(users.getSelectionModel()::select);
        }

        long activeCount = savedUsers.stream().filter(UserSummary::active).count();
        activeUsers.setText(Long.toString(activeCount));

        accountRecords.getItems().setAll(accounts.getAllAccounts());
        accountCount.setText(Integer.toString(accountRecords.getItems().size()));

        ledger.getItems().setAll(transactions.getBankHistory());
    }

    private Button button(String label, Action action) {
        Button button = new Button(label);
        button.setOnAction(event -> run(action));
        return button;
    }

    private void run(Action action) {
        try {
            status.setText("");
            action.run();
        } catch (SQLException | RuntimeException exception) {
            status.setText(exception.getMessage());
        }
    }

    @FunctionalInterface
    private interface Action {
        void run() throws SQLException;
    }

    public Parent getView() {
        return root;
    }
}