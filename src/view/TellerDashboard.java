package view;

import controller.AccountController;
import controller.CustomerController;
import controller.LoginController;
import controller.TransactionController;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;

public final class TellerDashboard {

    private final BorderPane root = new BorderPane();

    public TellerDashboard(Main main, LoginController login,
                           CustomerController customers, AccountController accounts,
                           TransactionController transactions) {
        HBox header = DashboardStyle.header(
                "Teller workspace", login.getCurrentUsername(), main::showLogin);
        header.setPadding(new Insets(12));
        root.setTop(header);

        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        RegisterCustomerView registrationView = new RegisterCustomerView(customers);
        Tab registration = new Tab("Register Customer", registrationView.getView());

        OpenAccountView opening = new OpenAccountView(accounts, customers);
        Tab open = new Tab("Open Account", opening.getView());

        TransactionView desk = new TransactionView(
                accounts,
                transactions,
                customers,
                "",
                "",
                () -> tabs.getSelectionModel().select(registration),
                customerId -> {
                    tabs.getSelectionModel().select(open);
                    opening.selectCustomer(customerId);
                }
        );

        Tab banking = new Tab("Transactions & History", desk.getView());

        banking.setOnSelectionChanged(event -> {
            if (banking.isSelected()) {
                desk.refreshOptions();
            }
        });

        open.setOnSelectionChanged(event -> {
            if (open.isSelected()) {
                opening.loadCustomers();
            }
        });

        tabs.getTabs().addAll(banking, registration, open);
        BorderPane.setMargin(tabs, new Insets(0, 12, 12, 12));
        root.setCenter(tabs);
    }

    public Parent getView() {
        return root;
    }
}