package dao;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

public class UserDAO {

    private static final int ITERATIONS = 600000;
    private static final int KEY_LENGTH = 256;
    private static final int SALT_LENGTH = 16;

    public record UserRecord(
            String userId, String username, String role, boolean active
    ) {}

    public String createUser(String username, String password, String role)
            throws SQLException {
        try (Connection connection = DatabaseManager.getConnection()) {
            return createUser(connection, username, password, role);
        }
    }

    public String createUser(Connection connection, String username,
                             String password, String role) throws SQLException {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username is required");
        }
        if (!List.of("CUSTOMER", "TELLER", "ADMIN").contains(role)) {
            throw new IllegalArgumentException("Invalid user role");
        }

        String userId = UUID.randomUUID().toString();
        String passwordHash = hashPassword(password);
        String sql = """
                INSERT INTO users (user_id, username, password_hash, role, active)
                VALUES (?, ?, ?, ?, 1)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, userId);
            statement.setString(2, username.trim());
            statement.setString(3, passwordHash);
            statement.setString(4, role);
            statement.executeUpdate();
        }

        return userId;
    }

    public UserRecord authenticate(String username, String password)
            throws SQLException {
        if (username == null || password == null) {
            return null;
        }

        String sql = """
                SELECT user_id, username, password_hash, role, active
                FROM users
                WHERE username = ?
                """;

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username.trim());

            try (ResultSet result = statement.executeQuery()) {
                if (!result.next() || result.getInt("active") != 1
                        || !verifyPassword(password, result.getString("password_hash"))) {
                    return null;
                }
                return mapUser(result);
            }
        }
    }

    public UserRecord findById(String userId) throws SQLException {
        String sql = """
                SELECT user_id, username, role, active FROM users WHERE user_id = ?
                """;

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, userId);

            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? mapUser(result) : null;
            }
        }
    }

    public List<UserRecord> findAll() throws SQLException {
        String sql = """
                SELECT user_id, username, role, active FROM users ORDER BY username
                """;
        List<UserRecord> users = new ArrayList<>();

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                users.add(mapUser(result));
            }
        }

        return users;
    }

    public boolean updatePassword(String userId, String password) throws SQLException {
        String passwordHash = hashPassword(password);
        String sql = "UPDATE users SET password_hash = ? WHERE user_id = ?";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, passwordHash);
            statement.setString(2, userId);
            return statement.executeUpdate() == 1;
        }
    }

    public boolean setActive(String userId, boolean active) throws SQLException {
        String sql = "UPDATE users SET active = ? WHERE user_id = ?";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, active ? 1 : 0);
            statement.setString(2, userId);
            return statement.executeUpdate() == 1;
        }
    }

    private UserRecord mapUser(ResultSet result) throws SQLException {
        return new UserRecord(
                result.getString("user_id"),
                result.getString("username"),
                result.getString("role"),
                result.getInt("active") == 1
        );
    }

    private String hashPassword(String password) {
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException(
                    "Password must contain at least 8 characters");
        }

        byte[] salt = new byte[SALT_LENGTH];
        new SecureRandom().nextBytes(salt);
        byte[] hash = deriveKey(password, salt, ITERATIONS);

        return ITERATIONS + ":"
                + Base64.getEncoder().encodeToString(salt) + ":"
                + Base64.getEncoder().encodeToString(hash);
    }

    private boolean verifyPassword(String password, String storedHash) {
        if (storedHash == null) {
            return false;
        }

        try {
            String[] parts = storedHash.split(":");
            if (parts.length != 3) {
                return false;
            }

            int iterations = Integer.parseInt(parts[0]);
            if (iterations < 100000 || iterations > 1000000) {
                return false;
            }

            byte[] salt = Base64.getDecoder().decode(parts[1]);
            byte[] expectedHash = Base64.getDecoder().decode(parts[2]);

            if (salt.length != SALT_LENGTH || expectedHash.length != KEY_LENGTH / 8) {
                return false;
            }

            byte[] actualHash = deriveKey(password, salt, iterations);
            return MessageDigest.isEqual(actualHash, expectedHash);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return false;
        }
    }

    private byte[] deriveKey(String password, byte[] salt, int iterations) {
        char[] characters = password.toCharArray();
        PBEKeySpec specification = new PBEKeySpec(
                characters, salt, iterations, KEY_LENGTH);

        try {
            SecretKeyFactory factory =
                    SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            return factory.generateSecret(specification).getEncoded();
        } catch (java.security.GeneralSecurityException exception) {
            throw new IllegalStateException("Password hashing failed", exception);
        } finally {
            specification.clearPassword();
            Arrays.fill(characters, '\0');
        }
    }
}