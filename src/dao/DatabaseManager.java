package dao;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseManager {

    private static final Path DATABASE_PATH =
            Path.of(System.getProperty("bank.database", "data/banking.db"))
                    .toAbsolutePath();

    public static Connection getConnection() throws SQLException {
        try {
            Files.createDirectories(DATABASE_PATH.getParent());
        } catch (IOException e) {
            throw new SQLException("Cannot create database directory", e);
        }

        Connection connection = DriverManager.getConnection(
                "jdbc:sqlite:" + DATABASE_PATH
        );

        try (var statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
            statement.execute("PRAGMA busy_timeout = 5000");
        } catch (SQLException e) {
            connection.close();
            throw e;
        }

        return connection;
    }
}