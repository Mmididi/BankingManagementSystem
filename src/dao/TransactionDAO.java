package dao;

import model.AccountRules;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TransactionDAO {

    private static final BigDecimal MAX_AMOUNT = AccountRules.MAX_BALANCE;

    public record TransactionRecord(
            String transactionId,
            String accountNumber,
            String transactionDate,
            String transactionType,
            BigDecimal amount,
            String createdBy,
            BigDecimal balanceAfter
    ) {}

    private record InterestAccount(String number, String type, BigDecimal balance) {}

    public void deposit(String accountNumber, BigDecimal amount, String createdBy)
            throws SQLException {
        processTransaction(accountNumber, amount, createdBy, false);
    }

    public boolean withdraw(String accountNumber, BigDecimal amount, String createdBy)
            throws SQLException {
        return processTransaction(accountNumber, amount, createdBy, true);
    }

    private boolean processTransaction(String accountNumber, BigDecimal amount,
                                       String createdBy, boolean withdrawal)
            throws SQLException {
        try (Connection connection = DatabaseManager.getConnection()) {
            connection.setAutoCommit(false);

            try {
                boolean posted = postTransaction(
                        connection, accountNumber, amount, createdBy, withdrawal);
                connection.commit();
                return posted;
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            }
        }
    }

    public boolean postTransaction(Connection connection, String accountNumber,
                                   BigDecimal amount, String createdBy,
                                   boolean withdrawal) throws SQLException {
        return postTransaction(connection, accountNumber, amount, createdBy, withdrawal,
                LocalDateTime.now(MonthlyInterestScheduler.BANK_ZONE));
    }

    boolean postTransaction(Connection connection, String accountNumber, BigDecimal amount,
                            String createdBy, boolean withdrawal, LocalDateTime postedAt)
            throws SQLException {
        BigDecimal value = validateAmount(amount);
        String selectSql = """
                SELECT account_type, balance FROM accounts WHERE account_number = ?
                """;

        String accountType;
        BigDecimal balance;
        try (PreparedStatement statement = connection.prepareStatement(selectSql)) {
            statement.setString(1, accountNumber);

            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new IllegalArgumentException("Account not found");
                }
                accountType = result.getString("account_type");
                balance = result.getBigDecimal("balance").setScale(2, RoundingMode.HALF_UP);
            }
        }

        BigDecimal newBalance = AccountRules.transactionBalance(
                accountType, balance, value, withdrawal);

        if (newBalance.signum() < 0) {
            return false;
        }

        updateBalance(connection, accountNumber, balance, newBalance);
        String type = withdrawal ? "WITHDRAWAL" : "DEPOSIT";
        saveTransaction(connection, accountNumber, type, value, createdBy, postedAt);

        return true;
    }

    public int applyMonthlyInterest() throws SQLException {
        LocalDateTime postedAt = LocalDateTime.now(MonthlyInterestScheduler.BANK_ZONE);
        String month = YearMonth.from(postedAt).toString();
        int processed = 0;

        try (Connection connection = DatabaseManager.getConnection()) {
            connection.setAutoCommit(false);

            try {
                for (InterestAccount account : findInterestAccounts(connection)) {
                    if (hasInterestForMonth(connection, account.number(), month)) {
                        continue;
                    }

                    BigDecimal interest = AccountRules.monthlyInterest(
                            account.type(), account.balance());
                    if (interest.signum() <= 0) {
                        continue;
                    }

                    BigDecimal newBalance = account.balance().add(interest);
                    if (newBalance.compareTo(MAX_AMOUNT) > 0) {
                        throw new SQLException("Interest would exceed account balance limit");
                    }

                    updateBalance(connection, account.number(), account.balance(), newBalance);
                    saveTransaction(connection, account.number(), "INTEREST", interest, null,
                            postedAt);
                    processed++;
                }

                connection.commit();
                return processed;
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            }
        }
    }

    private List<InterestAccount> findInterestAccounts(Connection connection)
            throws SQLException {
        String sql = """
                SELECT account_number, account_type, balance
                FROM accounts
                WHERE account_type IN ('SAVINGS', 'INVESTMENT')
                """;
        List<InterestAccount> accounts = new ArrayList<>();

        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                accounts.add(new InterestAccount(
                        result.getString("account_number"),
                        result.getString("account_type"),
                        result.getBigDecimal("balance").setScale(2, RoundingMode.HALF_UP)
                ));
            }
        }

        return accounts;
    }

    private boolean hasInterestForMonth(Connection connection, String accountNumber,
                                        String month) throws SQLException {
        String sql = """
                SELECT 1 FROM transactions
                WHERE account_number = ?
                  AND transaction_type = 'INTEREST'
                  AND substr(transaction_date, 1, 7) = ?
                LIMIT 1
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, accountNumber);
            statement.setString(2, month);

            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }

    private void updateBalance(Connection connection, String accountNumber,
                               BigDecimal oldBalance, BigDecimal newBalance)
            throws SQLException {
        String sql = "UPDATE accounts SET balance = ? WHERE account_number = ? AND balance = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setBigDecimal(1, newBalance);
            statement.setString(2, accountNumber);
            statement.setBigDecimal(3, oldBalance);

            if (statement.executeUpdate() != 1) {
                throw new SQLException("Account balance changed during transaction");
            }
        }
    }

    private void saveTransaction(Connection connection, String accountNumber, String type,
                                 BigDecimal amount, String createdBy, LocalDateTime date)
            throws SQLException {
        String sql = """
                INSERT INTO transactions
                    (transaction_id, account_number, transaction_date,
                     transaction_type, amount, created_by)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, UUID.randomUUID().toString());
            statement.setString(2, accountNumber);
            statement.setString(3, date.toString());
            statement.setString(4, type);
            statement.setBigDecimal(5, amount);
            statement.setString(6, createdBy);
            statement.executeUpdate();
        }
    }

    public List<TransactionRecord> findByAccountNumber(String accountNumber)
            throws SQLException {
        String sql = """
                SELECT * FROM transaction_ledger
                WHERE account_number = ?
                ORDER BY transaction_date DESC, transaction_id DESC
                """;

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, accountNumber);

            try (ResultSet result = statement.executeQuery()) {
                return readTransactions(result);
            }
        }
    }

    public List<TransactionRecord> findByCustomerId(String customerId)
            throws SQLException {
        String sql = """
                SELECT t.*
                FROM transaction_ledger t
                JOIN accounts a ON t.account_number = a.account_number
                WHERE a.customer_id = ?
                ORDER BY t.transaction_date DESC, t.transaction_id DESC
                """;

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, customerId);

            try (ResultSet result = statement.executeQuery()) {
                return readTransactions(result);
            }
        }
    }

    public List<TransactionRecord> findAll() throws SQLException {
        String sql = """
                SELECT * FROM transaction_ledger
                ORDER BY transaction_date DESC, transaction_id DESC
                """;

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            return readTransactions(result);
        }
    }

    private List<TransactionRecord> readTransactions(ResultSet result)
            throws SQLException {
        List<TransactionRecord> transactions = new ArrayList<>();

        while (result.next()) {
            transactions.add(new TransactionRecord(
                    result.getString("transaction_id"),
                    result.getString("account_number"),
                    result.getString("transaction_date"),
                    result.getString("transaction_type"),
                    result.getBigDecimal("amount").setScale(2, RoundingMode.HALF_UP),
                    result.getString("created_by"),
                    BigDecimal.valueOf(result.getLong("balance_after_cents"), 2)
            ));
        }

        return transactions;
    }

    private BigDecimal validateAmount(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount is required");
        }

        BigDecimal value;
        try {
            value = amount.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("Amount must have at most two decimal places");
        }

        if (value.signum() <= 0 || value.compareTo(MAX_AMOUNT) > 0) {
            throw new IllegalArgumentException("Invalid transaction amount");
        }

        return value;
    }
}