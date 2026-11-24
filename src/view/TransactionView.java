package view;

import controller.AccountController;
import controller.AccountController.AccountSummary;
import controller.CustomerController;
import controller.CustomerController.CustomerSummary;
import controller.TransactionController;
import controller.TransactionController.TransactionSummary;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public final class TransactionView {

    private final AccountController accounts;
    private final TransactionController transactions;
    private final CustomerController customers;
    private final boolean teller;

    private final HBox root = new HBox(14);
    private final TextField search = new TextField();
    private final ListView<CustomerSummary> directory = new ListView<>();
    private final ListView<AccountSummary> myAccounts = new ListView<>();

    private final TableView<AccountSummary> heldAccounts =
            DashboardStyle.table("Select a customer to see their accounts.");
    private final TableView<TransactionSummary> ledger =
            DashboardStyle.table("No transactions to show.");

    private final Label overviewTitle = DashboardStyle.label("Account overview", "dashboard-title");
    private final Label portfolio = DashboardStyle.label("", "muted");
    private final Label balance = DashboardStyle.label("—", "balance-value");
    private final Label branch = DashboardStyle.label("", "muted");
    private final Label rate = DashboardStyle.label("", "muted");
    private final Label policy = new Label("Savings accounts accept deposits. Withdrawals are unavailable.");
    private final Label message = new Label();

    private final TextField amount = new TextField();
    private final Button deposit =
            DashboardStyle.button("Deposit", "primary", () -> post(false));
    private final Button withdraw =
            DashboardStyle.button("Withdraw", "secondary", () -> post(true));
    private final CheckBox allHistory = new CheckBox("Show all accounts' transactions");

    private List<CustomerSummary> directoryEntries = List.of();
    private boolean refreshing;

    public TransactionView(AccountController accounts, TransactionController transactions,
                           CustomerController customers, String identity, String customerId,
                           Runnable register, Consumer<String> openAccount) {
        this.accounts = accounts;
        this.transactions = transactions;
        this.customers = customers;
        teller = customers != null;

        root.setPadding(new Insets(12));
        root.setStyle("-fx-background-color: #EDF3F7;");
        configureTables();
        configureActions();

        VBox sidebar = teller
                ? buildTellerSidebar(register)
                : buildCustomerSidebar(identity, customerId);

        sidebar.setPrefWidth(260);
        sidebar.setMinWidth(220);
        sidebar.setMaxHeight(Double.MAX_VALUE);

        VBox overview = DashboardStyle.card("", overviewTitle, portfolio, balance, branch, rate);
        overviewTitle.setWrapText(true);
        portfolio.setWrapText(true);
        HBox.setHgrow(overview, Priority.ALWAYS);

        VBox actions = DashboardStyle.card(
                teller ? "Teller transaction desk" : "Account actions",
                DashboardStyle.label("Amount (BWP)", "field-label"),
                amount, policy, new HBox(8, deposit, withdraw)
        );
        actions.setPrefWidth(270);

        VBox work = new VBox(12, new HBox(12, overview, actions));
        work.setPadding(new Insets(0, 4, 4, 0));

        if (teller) {
            Button open = DashboardStyle.button("Open account for customer", "primary", () -> {
                String selected = selectedCustomerId();
                if (selected == null) {
                    setMessage("Select a customer first.", true);
                } else {
                    openAccount.accept(selected);
                }
            });

            work.getChildren().add(
                    DashboardStyle.card("Held bank accounts", open, heldAccounts));
        }

        work.getChildren().addAll(
                message, DashboardStyle.card("Transaction ledger", allHistory, ledger));

        ScrollPane scroll = DashboardStyle.scroll(work);
        HBox.setHgrow(scroll, Priority.ALWAYS);
        root.getChildren().addAll(sidebar, scroll);

        refreshOptions();
    }

    private VBox buildTellerSidebar(Runnable register) {
        search.setId("customer-search");
        search.setPromptText("Search by name or customer ID");
        DashboardStyle.styleInput(search);
        search.textProperty().addListener((observable, previous, current) -> filterDirectory());

        directory.setId("transaction-customer");
        directory.setPlaceholder(new Label("No matching customers."));
        directory.getSelectionModel().selectedItemProperty().addListener(
                (observable, previous, current) -> {
                    if (!refreshing) {
                        refreshAccounts();
                    }
                });

        Button add = DashboardStyle.button("Register customer", "primary", register);
        add.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(directory, Priority.ALWAYS);
        return DashboardStyle.card("Customer directory", add, search, directory);
    }

    private VBox buildCustomerSidebar(String identity, String customerId) {
        myAccounts.setId("transaction-account");
        myAccounts.setPlaceholder(new Label("You have no accounts yet."));

        myAccounts.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(AccountSummary account, boolean empty) {
                super.updateItem(account, empty);
                setPadding(new Insets(7, 9, 7, 9));
                setText(empty || account == null ? null
                        : account.accountTypeName() + "\n" + account.accountNumber()
                        + " · " + DashboardStyle.bwp(account.balance()));
            }
        });

        myAccounts.getSelectionModel().selectedItemProperty().addListener(
                (observable, previous, current) -> {
                    if (!refreshing) {
                        refreshDetails();
                    }
                });

        VBox.setVgrow(myAccounts, Priority.ALWAYS);

        return DashboardStyle.card("Customer profile",
                DashboardStyle.label(identity, "field-label"),
                DashboardStyle.label(customerId, "muted"),
                new Separator(),
                DashboardStyle.label("My accounts", "section-title"),
                myAccounts
        );
    }

    private void configureTables() {
        heldAccounts.setId("transaction-account");
        heldAccounts.getColumns().setAll(List.of(
                DashboardStyle.column("Account number", AccountSummary::accountNumber),
                DashboardStyle.column("Type", AccountSummary::accountTypeName),
                DashboardStyle.column("Branch", AccountSummary::branch),
                DashboardStyle.column("Balance",
                        account -> DashboardStyle.bwp(account.balance()))
        ));
        heldAccounts.setPrefHeight(155);

        heldAccounts.getSelectionModel().selectedItemProperty().addListener(
                (observable, previous, current) -> {
                    if (!refreshing) {
                        refreshDetails();
                    }
                });

        ledger.setId("account-ledger");
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
        ledger.setPrefHeight(235);
    }

    private void configureActions() {
        amount.setId("transaction-amount");
        amount.setPromptText("0.00");
        DashboardStyle.styleInput(amount);
        deposit.setId("transaction-deposit");
        withdraw.setId("transaction-withdraw");

        policy.setWrapText(true);
        policy.setPadding(new Insets(8, 10, 8, 10));
        policy.setStyle("""
                -fx-background-color: #FFF3D8;
                -fx-background-radius: 7;
                -fx-text-fill: #765319;
                """);

        message.setWrapText(true);
        message.visibleProperty().bind(message.textProperty().isNotEmpty());
        message.managedProperty().bind(message.visibleProperty());

        allHistory.setSelected(!teller);
        allHistory.selectedProperty().addListener(
                (observable, previous, current) -> refreshDetails());
    }

    public void refreshOptions() {
        try {
            if (teller) {
                directoryEntries = customers.getAllCustomers();
                filterDirectory();
            } else {
                refreshAccounts();
            }
        } catch (SQLException | RuntimeException exception) {
            showError(exception);
        }
    }

    private void filterDirectory() {
        String query = search.getText().strip().toLowerCase(Locale.ROOT);
        String previousId = selectedCustomerId();

        refreshing = true;
        try {
            directory.getItems().setAll(directoryEntries.stream()
                    .filter(person -> person.toString().toLowerCase(Locale.ROOT).contains(query))
                    .toList());
            directory.getSelectionModel().clearSelection();

            if (previousId != null) {
                directory.getItems().stream()
                        .filter(person -> person.id().equals(previousId))
                        .findFirst()
                        .ifPresent(directory.getSelectionModel()::select);
            }
        } finally {
            refreshing = false;
        }

        refreshAccounts();
    }

    private String selectedCustomerId() {
        CustomerSummary person = directory.getSelectionModel().getSelectedItem();
        return person == null ? null : person.id();
    }

    private AccountSummary selectedAccount() {
        return teller
                ? heldAccounts.getSelectionModel().getSelectedItem()
                : myAccounts.getSelectionModel().getSelectedItem();
    }

    private void refreshAccounts() {
        AccountSummary previous = selectedAccount();
        var items = teller ? heldAccounts.getItems() : myAccounts.getItems();
        var selection = teller ? heldAccounts.getSelectionModel() : myAccounts.getSelectionModel();

        refreshing = true;
        try {
            List<AccountSummary> available;
            if (teller) {
                String customerId = selectedCustomerId();
                available = customerId == null ? List.of() : accounts.getCustomerAccounts(customerId);
            } else {
                available = accounts.getMyAccounts();
            }

            items.setAll(available);
            selection.clearSelection();

            if (previous != null) {
                items.stream()
                        .filter(account -> account.accountNumber().equals(previous.accountNumber()))
                        .findFirst()
                        .ifPresent(selection::select);
            }

            if (selection.isEmpty() && !items.isEmpty()) {
                selection.selectFirst();
            }

            CustomerSummary person = directory.getSelectionModel().getSelectedItem();
            if (person == null) {
                portfolio.setText("");
            } else {
                BigDecimal total = available.stream()
                        .map(AccountSummary::balance)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                portfolio.setText(person.name() + " · " + person.id()
                        + " · Total balance " + DashboardStyle.bwp(total));
            }
        } catch (SQLException | RuntimeException exception) {
            items.clear();
            portfolio.setText("");
            showError(exception);
        } finally {
            refreshing = false;
        }

        refreshDetails();
    }

    private void refreshDetails() {
        AccountSummary account = selectedAccount();
        boolean hasAccount = account != null;
        boolean withdrawalsAllowed = hasAccount && account.withdrawalAllowed();

        deposit.setDisable(!hasAccount);
        withdraw.setDisable(!withdrawalsAllowed);
        policy.setVisible(hasAccount && !withdrawalsAllowed);
        policy.setManaged(policy.isVisible());

        if (hasAccount) {
            overviewTitle.setText(account.accountTypeName() + " · " + account.accountNumber());
            balance.setText(DashboardStyle.bwp(account.balance()));
            branch.setText(account.branch());
            rate.setText(account.interestRateLabel());
        } else {
            overviewTitle.setText(teller ? "Choose a customer and account" : "Choose an account");
            balance.setText("—");
            branch.setText("");
            rate.setText("");
        }

        try {
            ledger.getItems().setAll(readHistory(account));
        } catch (SQLException | RuntimeException exception) {
            ledger.getItems().clear();
            showError(exception);
        }
    }

    private List<TransactionSummary> readHistory(AccountSummary account) throws SQLException {
        if (!allHistory.isSelected()) {
            return account == null ? List.of() : transactions.getHistory(account.accountNumber());
        }

        if (!teller) {
            return transactions.getMyHistory();
        }

        String customerId = selectedCustomerId();
        return customerId == null ? List.of() : transactions.getCustomerHistory(customerId);
    }

    private void post(boolean withdrawal) {
        AccountSummary account = selectedAccount();

        if (account == null) {
            setMessage("Select an account first.", true);
            return;
        }

        BigDecimal value;
        try {
            value = new BigDecimal(amount.getText().strip());
        } catch (NumberFormatException exception) {
            setMessage("Enter a valid amount in BWP.", true);
            return;
        }

        try {
            String customerId = selectedCustomerId();

            if (withdrawal) {
                boolean posted = transactions.withdraw(customerId, account.accountNumber(), value);
                if (!posted) {
                    setMessage("Withdrawal declined. Insufficient funds.", true);
                    return;
                }
            } else {
                transactions.deposit(customerId, account.accountNumber(), value);
            }

            amount.clear();
            setMessage(withdrawal ? "Withdrawal saved." : "Deposit saved.", false);
            refreshAccounts();
        } catch (SQLException | RuntimeException exception) {
            showError(exception);
        }
    }

    private void showError(Exception exception) {
        setMessage(exception.getMessage() == null
                ? "The operation could not be completed." : exception.getMessage(), true);
    }

    private void setMessage(String text, boolean error) {
        message.setStyle(error ? "-fx-text-fill: #A02632;" : "-fx-text-fill: #07634D;");
        message.setText(text);
    }

    public Parent getView() {
        return root;
    }
}