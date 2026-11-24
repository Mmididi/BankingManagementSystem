package dao;

import java.sql.*;

public class CustomerAccessDAO {

    public String findCustomerIdByUserId(String userId) throws SQLException {
        String sql = "SELECT customer_id FROM customers WHERE user_id = ?";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, userId);

            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getString("customer_id") : null;
            }
        }
    }

    public String findUsernameByCustomerId(String customerId) throws SQLException {
        String sql = """
                SELECT u.username
                FROM customers c
                JOIN users u ON u.user_id = c.user_id
                WHERE c.customer_id = ? AND u.role = 'CUSTOMER'
                """;

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, customerId);

            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getString("username") : null;
            }
        }
    }

    public boolean hasLogin(String customerId) throws SQLException {
        String sql = "SELECT user_id FROM customers WHERE customer_id = ?";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, customerId);

            try (ResultSet result = statement.executeQuery()) {
                return result.next() && result.getString("user_id") != null;
            }
        }
    }

    public String createLogin(String customerId, String username, String password,
                              UserDAO users) throws SQLException {
        String sql = """
                UPDATE customers SET user_id = ?
                WHERE customer_id = ? AND user_id IS NULL
                """;

        try (Connection connection = DatabaseManager.getConnection()) {
            connection.setAutoCommit(false);

            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                String userId = users.createUser(connection, username, password, "CUSTOMER");
                statement.setString(1, userId);
                statement.setString(2, customerId);

                if (statement.executeUpdate() != 1) {
                    throw new SQLException("Customer is missing or already has a login");
                }

                connection.commit();
                return userId;
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            }
        }
    }
}