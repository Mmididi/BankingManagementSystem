package view;

import controller.AccountController;
import controller.CustomerController;
import controller.CustomerController.CustomerSummary;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.sql.SQLException;

public final class OpenAccountView {

    private final AccountController accounts;
    private final CustomerController customers;

    private final ComboBox<CustomerSummary> customerBox = new ComboBox<>();
    private final Label status = new Label();
    private final ScrollPane root;

    private enum AccountType {
        SAVINGS("Savings Account"),
        INVESTMENT("Investment Account"),
        CHEQUE("Cheque Account");

        private final String label;

        AccountType(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public OpenAccountView(AccountController accounts, CustomerController customers) {
        this.accounts = accounts;
        this.customers = customers;

        Label title = new Label("Open customer bank account");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        customerBox.setPromptText("Select a registered customer");

        Button refresh = new Button("Refresh Customers");
        refresh.setOnAction(event -> loadCustomers());

        ComboBox<AccountType> accountType = new ComboBox<>();
        accountType.getItems().setAll(AccountType.values());
        accountType.setValue(AccountType.SAVINGS);

        TextField branch = new TextField("Main Branch");
        TextField initialDeposit = new TextField();
        initialDeposit.setPromptText("Enter opening deposit");

        Label requirements = new Label();
        requirements.setWrapText(true);
        status.setWrapText(true);

        Runnable updateForm = () -> {
            AccountType type = accountType.getValue();
            boolean investment = type == AccountType.INVESTMENT;

            initialDeposit.setDisable(!investment);
            initialDeposit.clear();

            requirements.setText(switch (type) {
                case SAVINGS -> "Savings accounts accept deposits. Withdrawals are unavailable.";
                case INVESTMENT -> "Opening deposit must be at least BWP 500.00.";
                case CHEQUE -> "The customer must be an employed individual with employer "
                        + "name and address on record.";
            });
        };

        accountType.setOnAction(event -> updateForm.run());
        updateForm.run();

        Button open = new Button("Open Account");
        open.setOnAction(event -> {
            try {
                CustomerSummary selected = customerBox.getValue();
                if (selected == null) {
                    throw new IllegalArgumentException("Select a registered customer.");
                }

                String branchName = branch.getText().strip();
                String accountNumber = switch (accountType.getValue()) {
                    case SAVINGS -> accounts.openSavingsAccount(selected.id(), branchName);
                    case INVESTMENT -> accounts.openInvestmentAccount(
                            selected.id(), branchName,
                            new BigDecimal(initialDeposit.getText().strip()));
                    case CHEQUE -> accounts.openChequeAccount(selected.id(), branchName);
                };

                status.setText(accountType.getValue() + " opened for " + selected.name()
                        + ".\nAccount number: " + accountNumber);
            } catch (NumberFormatException exception) {
                status.setText("Enter a valid opening deposit in BWP.");
            } catch (SQLException | RuntimeException exception) {
                status.setText(exception.getMessage());
            }
        });

        VBox form = new VBox(10,
                title,
                field("Registered customer", customerBox),
                refresh,
                field("Account type", accountType),
                field("Bank branch", branch),
                field("Opening deposit (BWP)", initialDeposit),
                requirements,
                open,
                status
        );
        form.setPadding(new Insets(16));
        form.setMaxWidth(650);

        root = new ScrollPane(form);
        root.setFitToWidth(true);
        loadCustomers();
    }

    public void loadCustomers() {
        CustomerSummary previous = customerBox.getValue();

        try {
            customerBox.getItems().setAll(customers.getAllCustomers());
            if (previous != null) {
                selectCustomer(previous.id());
            }
        } catch (SQLException | RuntimeException exception) {
            status.setText(exception.getMessage());
        }
    }

    public void selectCustomer(String customerId) {
        customerBox.getItems().stream()
                .filter(customer -> customer.id().equals(customerId))
                .findFirst()
                .ifPresent(customerBox.getSelectionModel()::select);
    }

    private VBox field(String name, Control input) {
        Label label = new Label(name);
        label.setLabelFor(input);
        input.setMaxWidth(Double.MAX_VALUE);
        return new VBox(4, label, input);
    }

    public Parent getView() {
        return root;
    }
}