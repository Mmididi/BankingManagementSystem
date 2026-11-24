package dao;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;

public class DatabaseInitializer {

    public static void initializeDatabase() throws SQLException {
        String sql;

        try (var input = DatabaseInitializer.class.getResourceAsStream("/schema.sql")) {
            if (input == null) {
                throw new SQLException("Missing schema.sql resource");
            }
            sql = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new SQLException("Cannot read schema.sql", exception);
        }

        try (var connection = DatabaseManager.getConnection();
             var statement = connection.createStatement()) {
            connection.setAutoCommit(false);

            try {
                for (String command : sql.split(";")) {
                    if (!command.isBlank()) {
                        statement.executeUpdate(command);
                    }
                }
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            }
        }
    }

    public static void main(String[] args) throws SQLException {
        initializeDatabase();

        if (Boolean.getBoolean("bank.demo")) {
            SampleDataSetup.initializeSampleUsers();
        }

        System.out.println("Banking database ready.");
    }
}