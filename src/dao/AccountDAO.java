package dao;

import model.AccountRules;
import model.EmployeeDetails;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AccountDAO {

    public record AccountRecord(
            String accountNumber,
            String customerId,
            String accountType,
            BigDecimal balance,
            String branch
    ) {}

    public String openSavingsAccount(String customerId, String branch)
            throws SQLException {
        return createAccount(customerId, branch, "SAVINGS", BigDecimal.ZERO);
    }

    public String openInvestmentAccount(String customerId, String branch,
                                        BigDecimal initialDeposit)
            throws SQLException {
        BigDecimal deposit = validateDeposit(initialDeposit);

        if (!AccountRules.meetsInvestmentMinimum(deposit)) {
            throw new IllegalArgumentException(
                    "Investment accounts require at least BWP500");
        }

        return createAccount(customerId, branch, "INVESTMENT", deposit);
    }

    public String openChequeAccount(String customerId, String branch)
            throws SQLException {
        return createAccount(customerId, branch, "CHEQUE", BigDecimal.ZERO);
    }

    private String createAccount(String customerId, String branch,
                                 String accountType, BigDecimal initialBalance)
            throws SQLException {
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("Customer ID is required");
        }
        if (branch == null || branch.isBlank()) {
            throw new IllegalArgumentException("Branch is required");
        }

        String accountNumber = "ACC-" + UUID.randomUUID().toString()
                .replace("-", "").substring(0, 16).toUpperCase();

        String sql = """
                INSERT INTO accounts
                    (account_number, customer_id, account_type, balance, branch)
                VALUES (?, ?, ?, ?, ?)
                """;

        try (Connection connection = DatabaseManager.getConnection()) {
            connection.setAutoCommit(false);

            try {
                verifyCustomer(connection, customerId, accountType);

                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setString(1, accountNumber);
                    statement.setString(2, customerId);
                    statement.setString(3, accountType);
                    statement.setBigDecimal(4, initialBalance);
                    statement.setString(5, branch);
                    statement.executeUpdate();
                }

                if (initialBalance.signum() > 0) {
                    recordOpeningDeposit(connection, accountNumber, initialBalance);
                }

                connection.commit();
                return accountNumber;
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            }
        }
    }

    private void recordOpeningDeposit(Connection connection, String accountNumber,
                                      BigDecimal amount) throws SQLException {
        String sql = """
                INSERT INTO transactions
                    (transaction_id, account_number, transaction_date,
                     transaction_type, amount, created_by)
                VALUES (?, ?, ?, 'DEPOSIT', ?, NULL)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, UUID.randomUUID().toString());
            statement.setString(2, accountNumber);
            statement.setString(3, LocalDateTime.now().toString());
            statement.setBigDecimal(4, amount);
            statement.executeUpdate();
        }
    }

    private void verifyCustomer(Connection connection, String customerId,
                                String accountType) throws SQLException {
        String sql = """
                SELECT c.customer_type, e.employed,
                       e.employer_name, e.employer_address
                FROM customers c
                LEFT JOIN employment_details e ON c.customer_id = e.customer_id
                WHERE c.customer_id = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, customerId);

            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new IllegalArgumentException(
                            "Customer must be registered first");
                }

                if ("CHEQUE".equals(accountType)) {
                    boolean individual = "INDIVIDUAL".equals(
                            result.getString("customer_type"));
                    EmployeeDetails details = new EmployeeDetails(
                            result.getString("employer_name"),
                            result.getString("employer_address"),
                            "",
                            result.getInt("employed") == 1
                    );

                    if (!AccountRules.canOpenCheque(individual, details)) {
                        throw new IllegalArgumentException(
                                "Cheque accounts require verified employment");
                    }
                }
            }
        }
    }

    public AccountRecord findByAccountNumber(String accountNumber)
            throws SQLException {
        String sql = "SELECT * FROM accounts WHERE account_number = ?";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, accountNumber);

            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? mapAccount(result) : null;
            }
        }
    }

    public List<AccountRecord> findByCustomerId(String customerId)
            throws SQLException {
        String sql = """
                SELECT * FROM accounts
                WHERE customer_id = ?
                ORDER BY account_number
                """;

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, customerId);

            try (ResultSet result = statement.executeQuery()) {
                return readAccounts(result);
            }
        }
    }

    public List<AccountRecord> findAll() throws SQLException {
        String sql = "SELECT * FROM accounts ORDER BY account_number";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            return readAccounts(result);
        }
    }

    public BigDecimal getBalance(String accountNumber) throws SQLException {
        AccountRecord account = findByAccountNumber(accountNumber);

        if (account == null) {
            throw new IllegalArgumentException("Account not found");
        }

        return account.balance();
    }

    private List<AccountRecord> readAccounts(ResultSet result)
            throws SQLException {
        List<AccountRecord> accounts = new ArrayList<>();

        while (result.next()) {
            accounts.add(mapAccount(result));
        }

        return accounts;
    }

    private AccountRecord mapAccount(ResultSet result) throws SQLException {
        return new AccountRecord(
                result.getString("account_number"),
                result.getString("customer_id"),
                result.getString("account_type"),
                result.getBigDecimal("balance").setScale(2, RoundingMode.HALF_UP),
                result.getString("branch")
        );
    }

    private BigDecimal validateDeposit(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount is required");
        }

        BigDecimal value;
        try {
            value = amount.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException(
                    "Amounts must have at most two decimal places");
        }

        if (value.signum() <= 0
                || value.compareTo(new BigDecimal("999999999999.99")) > 0) {
            throw new IllegalArgumentException("Invalid amount");
        }

        return value;
    }
}