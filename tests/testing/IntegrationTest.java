package testing;

import controller.CustomerController.CustomerSummary;
import controller.TransactionController.TransactionSummary;
import dao.AccountDAO;
import dao.CustomerDAO;
import dao.DatabaseInitializer;
import dao.SampleDataSetup;
import dao.TransactionDAO;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import model.IndividualCustomer;
import view.Main;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

public final class IntegrationTest {

    private static final String CUSTOMER_ID = "CUS-101";
    private static final String ACCOUNT_NUMBER = "100000101";
    private static final BigDecimal DEPOSIT = new BigDecimal("75.00");

    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory("banking-integration-");
        Path database = directory.resolve("integration.db");
        System.setProperty("bank.database", database.toString());
        System.setProperty("bank.demo", "true");

        try {
            DatabaseInitializer.initializeDatabase();
            SampleDataSetup.initializeSampleUsers();
            verifyCustomerCrud();

            Platform.startup(() -> Platform.setImplicitExit(false));
            try {
                FutureTask<Void> scenario = new FutureTask<>(() -> {
                    verifyGuiTransaction();
                    return null;
                });
                Platform.runLater(scenario);
                scenario.get(45, TimeUnit.SECONDS);
            } finally {
                Platform.exit();
            }
        } finally {
            Files.deleteIfExists(database);
            Files.deleteIfExists(directory);
        }

        System.out.println("All integration checks passed.");
    }

    private static void verifyCustomerCrud() throws Exception {
        CustomerDAO customers = new CustomerDAO();
        int originalCount = customers.findAll().size();
        require(originalCount == 10, "Expected 10 seeded customers");

        String id = customers.generateCustomerId();
        require("CUS01".equals(id), "Expected the first new customer ID to be CUS01");

        var customer = new IndividualCustomer(
                id, "Test", "Customer", "Gaborone", "Male",
                "test@example.test", "70000000", "TEST-IDENTITY"
        );
        customers.registerCustomer(customer);
        require(customers.findById(id) instanceof IndividualCustomer,
                "Customer must be readable after registration");

        require(customers.updateIndividualContact(id, "Francistown",
                "updated@example.test", "70000001"), "Customer update failed");
        var updated = (IndividualCustomer) customers.findById(id);
        require("Francistown".equals(updated.getAddress())
                        && "updated@example.test".equals(updated.getEmail())
                        && "70000001".equals(updated.getCellNo()),
                "Updated contact details were not persisted");

        require(customers.deleteCustomer(id), "Temporary customer deletion failed");
        require(customers.findById(id) == null && customers.findAll().size() == originalCount,
                "Customer remained after deletion");

        System.out.println("PASS: 10 seeded customers and customer CRUD");
    }

    private static void verifyGuiTransaction() throws Exception {
        AccountDAO accounts = new AccountDAO();
        TransactionDAO transactions = new TransactionDAO();
        BigDecimal expectedBalance = accounts.getBalance(ACCOUNT_NUMBER).add(DEPOSIT);
        int historyCount = transactions.findByAccountNumber(ACCOUNT_NUMBER).size();

        Main app = new Main();
        Stage stage = new Stage();
        try {
            app.start(stage);
            Scene scene = stage.getScene();
            find(scene, "#portal-staff", ToggleButton.class).fire();

            signIn(scene, SampleDataSetup.TELLER_USERNAME, "WrongPassword");
            require(!find(scene, "#login-error", Label.class).getText().isBlank(),
                    "Sign-in should reject an incorrect password");

            signIn(scene, SampleDataSetup.TELLER_USERNAME, SampleDataSetup.PASSWORD);
            ListView<?> directory = find(scene, "#transaction-customer", ListView.class);
            int customerIndex = -1;
            for (int index = 0; index < directory.getItems().size(); index++) {
                if (directory.getItems().get(index) instanceof CustomerSummary customer
                        && CUSTOMER_ID.equals(customer.id())) {
                    customerIndex = index;
                    break;
                }
            }
            require(customerIndex >= 0, "Seeded customer was not in the teller directory");
            directory.getSelectionModel().select(customerIndex);
            require(find(scene, "#transaction-withdraw", Button.class).isDisabled(),
                    "Savings withdrawals should be disabled");

            find(scene, "#transaction-amount", TextField.class).setText(DEPOSIT.toPlainString());
            find(scene, "#transaction-deposit", Button.class).fire();

            require(expectedBalance.compareTo(accounts.getBalance(ACCOUNT_NUMBER)) == 0,
                    "The deposit did not update the saved balance");
            var history = transactions.findByAccountNumber(ACCOUNT_NUMBER);
            require(history.size() == historyCount + 1,
                    "Exactly one new ledger entry should be saved");

            var entry = history.getFirst();
            require("DEPOSIT".equals(entry.transactionType())
                            && DEPOSIT.compareTo(entry.amount()) == 0
                            && expectedBalance.compareTo(entry.balanceAfter()) == 0,
                    "The saved ledger entry does not match the deposit");

            app.showLogin();
            signIn(scene, CUSTOMER_ID, SampleDataSetup.PASSWORD);
            TableView<?> ledger = find(scene, "#account-ledger", TableView.class);

            require(ledger.getItems().stream().anyMatch(row ->
                            row instanceof TransactionSummary transaction
                                    && entry.transactionId().equals(transaction.transactionId())),
                    "The customer dashboard did not display the saved teller deposit");

            System.out.println("PASS: GUI login rejection, teller deposit, SQLite persistence, "
                    + "Savings withdrawal restriction, and customer ledger");
        } finally {
            app.stop();
            stage.close();
        }
    }

    private static void signIn(Scene scene, String username, String password) {
        find(scene, "#login-username", TextField.class).setText(username);
        find(scene, "#login-password", PasswordField.class).setText(password);
        find(scene, "#login-submit", Button.class).fire();
    }

    private static <T> T find(Scene scene, String selector, Class<T> type) {
        scene.getRoot().applyCss();
        scene.getRoot().layout();
        Object node = scene.lookup(selector);
        require(type.isInstance(node), "Missing JavaFX control: " + selector);
        return type.cast(node);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
