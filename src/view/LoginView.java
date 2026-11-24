package view;

import controller.LoginController;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public final class LoginView {

    private final BorderPane root = new BorderPane();

    public LoginView(Main main, LoginController login) {
        HBox header = new HBox(
                DashboardStyle.label("Banking Management System", "brand-title"));
        DashboardStyle.styleHeader(header);
        root.setTop(header);

        ToggleGroup portals = new ToggleGroup();
        ToggleButton customer = new ToggleButton("Customer");
        ToggleButton staff = new ToggleButton("Teller / Administrator");
        customer.setId("portal-customer");
        staff.setId("portal-staff");

        for (ToggleButton option : new ToggleButton[]{customer, staff}) {
            option.setToggleGroup(portals);
            option.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(option, Priority.ALWAYS);
        }

        customer.setSelected(true);
        stylePortal(customer);
        stylePortal(staff);

        HBox choices = new HBox(6, customer, staff);
        choices.setPadding(new Insets(4));
        choices.setStyle("-fx-background-color: #E7EFF4; -fx-background-radius: 8;");

        TextField username = new TextField();
        username.setId("login-username");
        username.setPromptText("Enter your username or customer ID");
        DashboardStyle.styleInput(username);

        Label usernameLabel = DashboardStyle.label("Username or customer ID", "field-label");
        usernameLabel.setLabelFor(username);

        PasswordField password = new PasswordField();
        password.setId("login-password");
        password.setPromptText("Enter your password");
        DashboardStyle.styleInput(password);

        Label passwordLabel = DashboardStyle.label("Password", "field-label");
        passwordLabel.setLabelFor(password);

        Label message = DashboardStyle.label("", "error-text");
        message.setId("login-error");
        message.setWrapText(true);
        message.visibleProperty().bind(message.textProperty().isNotEmpty());
        message.managedProperty().bind(message.visibleProperty());

        portals.selectedToggleProperty().addListener((observable, previous, selected) -> {
            if (selected == null && previous != null) {
                previous.setSelected(true);
                return;
            }

            stylePortal(customer);
            stylePortal(staff);

            boolean staffPortal = staff.isSelected();
            usernameLabel.setText(staffPortal ? "Staff username" : "Username or customer ID");
            username.setPromptText(staffPortal
                    ? "e.g. Tsotlhe Mokakangwe"
                    : "Enter your username or customer ID");
            message.setText("");
        });

        Button submit = DashboardStyle.button("Sign in", "primary", () -> {
            try {
                boolean signedIn = login.loginForPortal(
                        username.getText(), password.getText(), customer.isSelected());

                if (!signedIn) {
                    message.setText("Sign-in failed. Check your details and selected portal.");
                    password.clear();
                    return;
                }

                switch (login.getCurrentRole()) {
                    case "CUSTOMER" -> main.showCustomerDashboard();
                    case "TELLER" -> main.showTellerDashboard();
                    case "ADMIN" -> main.showAdminDashboard();
                    default -> throw new IllegalStateException("Account role is unavailable.");
                }
            } catch (Exception exception) {
                login.logout();
                password.clear();
                message.setText("Sign-in could not be completed.");
            }
        });

        submit.setId("login-submit");
        submit.setMaxWidth(Double.MAX_VALUE);
        submit.setDefaultButton(true);

        VBox form = DashboardStyle.card("",
                DashboardStyle.label("Sign in to banking", "login-title"),
                choices,
                new VBox(5, usernameLabel, username),
                new VBox(5, passwordLabel, password),
                submit,
                message
        );
        form.setPadding(new Insets(26));
        form.setSpacing(14);
        form.setMaxWidth(485);
        form.setMaxHeight(Region.USE_PREF_SIZE);

        StackPane page = new StackPane(form);
        page.setAlignment(Pos.CENTER);
        page.setPadding(new Insets(24));
        page.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, #163C51, #0B2231);");
        root.setCenter(page);
    }

    private void stylePortal(ToggleButton option) {
        String background = option.isSelected() ? "white" : "transparent";

        option.setStyle(
                "-fx-background-color: " + background + ";"
                        + "-fx-background-radius: 6;"
                        + "-fx-text-fill: #18394A;"
                        + "-fx-font-weight: bold;"
                        + "-fx-padding: 8 12;"
                        + "-fx-cursor: hand;"
        );
    }

    public Parent getView() {
        return root;
    }
}