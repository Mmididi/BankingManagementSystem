package view;

import controller.AccountController;
import controller.LoginController;
import controller.TransactionController;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;

import java.sql.SQLException;

public final class CustomerDashboard {

    private final BorderPane root = new BorderPane();

    public CustomerDashboard(Main main, LoginController login,
                             AccountController accounts,
                             TransactionController transactions) {
        HBox header = DashboardStyle.header(
                "Customer portal", login.getCurrentUsername(), main::showLogin);
        header.setPadding(new Insets(12));
        root.setTop(header);

        try {
            String customerId = login.getCurrentCustomerId();
            TransactionView customerView = new TransactionView(
                    accounts, transactions, null, login.getCurrentUsername(),
                    customerId, null, null
            );

            Parent content = customerView.getView();
            BorderPane.setMargin(content, new Insets(12));
            root.setCenter(content);
        } catch (SQLException | SecurityException exception) {
            root.setCenter(DashboardStyle.card(
                    "Account unavailable",
                    DashboardStyle.label(exception.getMessage(), "error-text")
            ));
        }
    }

    public Parent getView() {
        return root;
    }
}