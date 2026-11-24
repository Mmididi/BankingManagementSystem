package controller;

import dao.UserDAO;

import java.sql.SQLException;
import java.util.List;

public class AdminController {

    private final LoginController loginController;
    private final UserDAO userDAO = new UserDAO();

    public AdminController(LoginController loginController) {
        if (loginController == null) {
            throw new IllegalArgumentException("Login controller is required");
        }
        this.loginController = loginController;
    }

    public String createTeller(String username, String password) throws SQLException {
        loginController.requireRole("ADMIN");
        return userDAO.createUser(username, password, "TELLER");
    }

    public record UserSummary(String id, String username, String role, boolean active) {
        @Override
        public String toString() {
            return username + " | " + role + " | " + (active ? "Active" : "Inactive");
        }
    }

    public List<UserSummary> getAllUsers() throws SQLException {
        loginController.requireRole("ADMIN");

        return userDAO.findAll().stream()
                .map(user -> new UserSummary(
                        user.userId(), user.username(), user.role(), user.active()))
                .toList();
    }

    public boolean deactivateUser(String userId) throws SQLException {
        loginController.requireRole("ADMIN");

        if (loginController.getCurrentUserId().equals(userId)) {
            throw new SecurityException("You cannot deactivate your own administrator account");
        }

        return userDAO.setActive(userId, false);
    }

    public boolean activateUser(String userId) throws SQLException {
        loginController.requireRole("ADMIN");
        return userDAO.setActive(userId, true);
    }

    public boolean resetUserPassword(String userId, String newPassword)
            throws SQLException {
        loginController.requireRole("ADMIN");
        return userDAO.updatePassword(userId, newPassword);
    }
}