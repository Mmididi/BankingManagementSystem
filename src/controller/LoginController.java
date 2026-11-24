package controller;

import dao.CustomerAccessDAO;
import dao.UserDAO;

import java.sql.SQLException;
import java.util.List;

public class LoginController {

    private final UserDAO userDAO = new UserDAO();
    private final CustomerAccessDAO customerAccessDAO = new CustomerAccessDAO();
    private UserDAO.UserRecord currentUser;

    public boolean login(String username, String password) throws SQLException {
        currentUser = null;

        if (username == null || username.isBlank()
                || password == null || password.isBlank()) {
            return false;
        }

        currentUser = userDAO.authenticate(username, password);
        return currentUser != null;
    }

    public boolean loginForPortal(String identifier, String password, boolean customerPortal)
            throws SQLException {
        String username = identifier == null ? "" : identifier.strip();

        if (customerPortal) {
            String linkedUsername = customerAccessDAO.findUsernameByCustomerId(username);
            if (linkedUsername != null) {
                username = linkedUsername;
            }
        }

        if (!login(username, password)) {
            return false;
        }

        if (customerPortal != isCustomer()) {
            logout();
            return false;
        }

        return true;
    }

    public void logout() {
        currentUser = null;
    }

    public boolean isLoggedIn() {
        return currentUser != null && currentUser.active();
    }

    public UserDAO.UserRecord getCurrentUser() {
        return currentUser;
    }

    public String getCurrentRole() {
        return isLoggedIn() ? currentUser.role() : null;
    }

    public String getCurrentUserId() {
        return isLoggedIn() ? currentUser.userId() : null;
    }

    public String getCurrentUsername() {
        return isLoggedIn() ? currentUser.username() : null;
    }

    public boolean isCustomer() {
        return hasRole("CUSTOMER");
    }

    public boolean isTeller() {
        return hasRole("TELLER");
    }

    public boolean isAdministrator() {
        return hasRole("ADMIN");
    }

    public boolean hasRole(String role) {
        return isLoggedIn() && currentUser.role().equals(role);
    }

    public void requireRole(String... roles) throws SQLException {
        if (!isLoggedIn()) {
            throw new SecurityException("Please log in first");
        }

        UserDAO.UserRecord latest = userDAO.findById(currentUser.userId());

        if (latest == null || !latest.active()) {
            logout();
            throw new SecurityException("Your account is inactive");
        }

        if (!List.of(roles).contains(latest.role())) {
            throw new SecurityException("You are not authorised to perform this operation");
        }

        currentUser = latest;
    }

    public String getCurrentCustomerId() throws SQLException {
        requireRole("CUSTOMER");

        String customerId = customerAccessDAO.findCustomerIdByUserId(currentUser.userId());

        if (customerId == null) {
            throw new SecurityException("No customer record is linked to this login");
        }

        return customerId;
    }

    public void requireAccountAccess(String customerId) throws SQLException {
        requireRole("CUSTOMER", "TELLER", "ADMIN");

        if (isCustomer() && !getCurrentCustomerId().equals(customerId)) {
            throw new SecurityException("You cannot access another customer's account");
        }
    }

    public String createInitialAdministrator(String username, String password)
            throws SQLException {
        if (!userDAO.findAll().isEmpty()) {
            throw new SecurityException(
                    "Initial setup is only available for an empty user database");
        }

        return userDAO.createUser(username, password, "ADMIN");
    }
}