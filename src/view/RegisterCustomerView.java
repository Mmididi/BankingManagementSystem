package view;

import controller.CustomerController;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.sql.SQLException;

public final class RegisterCustomerView {

    private final CustomerController customers;
    private final TabPane root = new TabPane();

    public RegisterCustomerView(CustomerController customers) {
        this.customers = customers;
        root.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        root.getTabs().addAll(
                individualTab(), organisationTab(), employmentTab(), loginTab());
    }

    private Tab individualTab() {
        TextField firstName = new TextField();
        TextField surname = new TextField();
        TextField address = new TextField();
        TextField email = new TextField();
        TextField cellphone = new TextField();
        TextField identityNumber = new TextField();

        ComboBox<String> gender = new ComboBox<>();
        gender.getItems().addAll("Male", "Female", "Other");
        gender.setPromptText("Select gender");

        Label status = new Label();
        Button register = action("Register Individual", status, () -> {
            String customerId = customers.registerIndividual(
                    required(firstName, "First name"),
                    required(surname, "Surname"),
                    text(address),
                    gender.getValue() == null ? "" : gender.getValue(),
                    text(email),
                    text(cellphone),
                    text(identityNumber)
            );

            for (TextField field : new TextField[]{
                    firstName, surname, address, email, cellphone, identityNumber
            }) {
                field.clear();
            }
            gender.setValue(null);

            return "Customer registered. Customer ID: " + customerId;
        });

        return formTab("Individual", "Register individual customer",
                field("First name", firstName),
                field("Surname", surname),
                field("Residential address", address),
                field("Gender", gender),
                field("Email address", email),
                field("Cellphone number", cellphone),
                field("Identity number", identityNumber),
                register, status
        );
    }

    private Tab organisationTab() {
        TextField name = new TextField();
        TextField registrationNumber = new TextField();
        TextField address = new TextField();

        Label status = new Label();
        Button register = action("Register Organisation", status, () -> {
            String customerId = customers.registerOrganisation(
                    required(name, "Organisation name"),
                    text(registrationNumber),
                    text(address)
            );

            name.clear();
            registrationNumber.clear();
            address.clear();

            return "Organisation registered. Customer ID: " + customerId;
        });

        return formTab("Organisation", "Register organisation customer",
                field("Organisation name", name),
                field("Registration number", registrationNumber),
                field("Business address", address),
                register, status
        );
    }

    private Tab employmentTab() {
        TextField customerId = new TextField();
        TextField employer = new TextField();
        TextField employerAddress = new TextField();
        TextField jobTitle = new TextField();
        CheckBox employed = new CheckBox("Customer is currently employed");

        employer.disableProperty().bind(employed.selectedProperty().not());
        employerAddress.disableProperty().bind(employed.selectedProperty().not());
        jobTitle.disableProperty().bind(employed.selectedProperty().not());

        Label status = new Label();
        Button save = action("Save Employment Details", status, () -> {
            boolean working = employed.isSelected();

            customers.updateEmploymentDetails(
                    required(customerId, "Customer ID"),
                    working ? required(employer, "Employer name") : "",
                    working ? required(employerAddress, "Employer address") : "",
                    working ? text(jobTitle) : "",
                    working
            );

            return "Employment details saved.";
        });

        return formTab("Employment", "Customer employment details",
                new Label("Record employment details before opening a Cheque Account."),
                field("Customer ID", customerId),
                employed,
                field("Employer name", employer),
                field("Employer address", employerAddress),
                field("Job title", jobTitle),
                save, status
        );
    }

    private Tab loginTab() {
        TextField customerId = new TextField();
        TextField username = new TextField();
        PasswordField password = new PasswordField();
        password.setPromptText("At least 8 characters");

        Label status = new Label();
        Button create = action("Create Customer Login", status, () -> {
            String loginName = required(username, "Username");

            customers.createCustomerLogin(
                    required(customerId, "Customer ID"), loginName, password.getText());

            password.clear();
            return "Customer login created for " + loginName + ".";
        });

        return formTab("Customer Login", "Create customer login",
                field("Registered customer ID", customerId),
                field("Username", username),
                field("Password", password),
                create, status
        );
    }

    private Tab formTab(String tabName, String heading, Node... controls) {
        Label title = new Label(heading);
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        VBox form = new VBox(10, title);
        form.setPadding(new Insets(16));
        form.setMaxWidth(600);
        form.getChildren().addAll(controls);

        ScrollPane scroll = new ScrollPane(form);
        scroll.setFitToWidth(true);
        return new Tab(tabName, scroll);
    }

    private VBox field(String name, Control input) {
        Label label = new Label(name);
        label.setLabelFor(input);
        input.setMaxWidth(Double.MAX_VALUE);
        return new VBox(4, label, input);
    }

    private Button action(String caption, Label status, FormAction operation) {
        status.setWrapText(true);

        Button button = new Button(caption);
        button.setOnAction(event -> {
            try {
                status.setText(operation.run());
            } catch (SQLException | RuntimeException exception) {
                String message = exception.getMessage();
                status.setText(message == null ? "The operation failed." : message);
            }
        });

        return button;
    }

    private String text(TextField field) {
        return field.getText().strip();
    }

    private String required(TextField field, String name) {
        String value = text(field);
        if (value.isEmpty()) {
            throw new IllegalArgumentException(name + " is required.");
        }
        return value;
    }

    @FunctionalInterface
    private interface FormAction {
        String run() throws SQLException;
    }

    public Parent getView() {
        return root;
    }
}