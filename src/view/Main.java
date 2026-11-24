package view;

import controller.AccountController;
import controller.AdminController;
import controller.CustomerController;
import controller.LoginController;
import controller.TransactionController;
import dao.DatabaseInitializer;
import dao.MonthlyInterestScheduler;
import dao.SampleDataSetup;
import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    private Stage primaryStage;

    private final MonthlyInterestScheduler scheduler =
            new MonthlyInterestScheduler();

    private final LoginController loginController =
            new LoginController();

    private final CustomerController customerController =
            new CustomerController(loginController);

    private final AccountController accountController =
            new AccountController(loginController);

    private final TransactionController transactionController =
            new TransactionController(loginController);

    private final AdminController adminController =
            new AdminController(loginController);

    @Override
    public void start(Stage stage) throws Exception {
        DatabaseInitializer.initializeDatabase();

        if (Boolean.getBoolean("bank.demo")) {
            SampleDataSetup.initializeSampleUsers();
        }

        primaryStage = stage;
        primaryStage.setTitle("Banking Management System");
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(600);

        showLogin();
        primaryStage.show();
        scheduler.start();
    }

    @Override
    public void stop() {
        scheduler.close();
    }

    public void showLogin() {
        loginController.logout();

        LoginView loginView = new LoginView(this, loginController);
        showScene(loginView.getView());
    }

    public void showCustomerDashboard() {
        CustomerDashboard dashboard = new CustomerDashboard(
                this, loginController, accountController, transactionController
        );

        showScene(dashboard.getView());
    }

    public void showTellerDashboard() {
        TellerDashboard dashboard = new TellerDashboard(
                this, loginController, customerController,
                accountController, transactionController
        );

        showScene(dashboard.getView());
    }

    public void showAdminDashboard() {
        AdminDashboard dashboard = new AdminDashboard(
                this, loginController, adminController,
                accountController, transactionController
        );

        showScene(dashboard.getView());
    }

    private void showScene(Parent root) {
        Scene scene = primaryStage.getScene();

        if (scene == null) {
            primaryStage.setScene(new Scene(root, 1100, 720));
        } else {
            scene.setRoot(root);
        }
    }
}