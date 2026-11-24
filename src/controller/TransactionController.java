package controller;

import dao.AccountDAO;
import dao.TransactionDAO;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

public class TransactionController {

    private final LoginController loginController;
    private final AccountDAO accountDAO = new AccountDAO();
    private final TransactionDAO transactionDAO = new TransactionDAO();

    public TransactionController(LoginController loginController) {
        if (loginController == null) {
            throw new IllegalArgumentException("Login controller is required");
        }
        this.loginController = loginController;
    }

    public void deposit(String accountNumber, BigDecimal amount) throws SQLException {
        deposit(null, accountNumber, amount);
    }

    public void deposit(String customerId, String accountNumber, BigDecimal amount)
            throws SQLException {
        AccountDAO.AccountRecord account = verifyTransactionOwner(customerId, accountNumber);
        String tellerId = loginController.isTeller() ? loginController.getCurrentUserId() : null;

        transactionDAO.deposit(account.accountNumber(), amount, tellerId);
    }

    public boolean withdraw(String accountNumber, BigDecimal amount) throws SQLException {
        return withdraw(null, accountNumber, amount);
    }

    public boolean withdraw(String customerId, String accountNumber, BigDecimal amount)
            throws SQLException {
        AccountDAO.AccountRecord account = verifyTransactionOwner(customerId, accountNumber);
        String tellerId = loginController.isTeller() ? loginController.getCurrentUserId() : null;

        return transactionDAO.withdraw(account.accountNumber(), amount, tellerId);
    }

    public record TransactionSummary(String transactionId, String accountNumber,
                                     String transactionDate, String transactionType,
                                     BigDecimal amount, String createdBy, BigDecimal balanceAfter) {

        private static TransactionSummary from(TransactionDAO.TransactionRecord row) {
            return new TransactionSummary(
                    row.transactionId(), row.accountNumber(), row.transactionDate(),
                    row.transactionType(), row.amount(), row.createdBy(), row.balanceAfter()
            );
        }
    }

    public List<TransactionSummary> getHistory(String accountNumber) throws SQLException {
        verifyAccountAccess(accountNumber);

        return transactionDAO.findByAccountNumber(accountNumber).stream()
                .map(TransactionSummary::from)
                .toList();
    }

    public List<TransactionSummary> getCustomerHistory(String customerId)
            throws SQLException {
        loginController.requireAccountAccess(customerId);

        return transactionDAO.findByCustomerId(customerId).stream()
                .map(TransactionSummary::from)
                .toList();
    }

    public List<TransactionSummary> getMyHistory() throws SQLException {
        String customerId = loginController.getCurrentCustomerId();

        return transactionDAO.findByCustomerId(customerId).stream()
                .map(TransactionSummary::from)
                .toList();
    }

    public List<TransactionSummary> getBankHistory() throws SQLException {
        loginController.requireRole("ADMIN");

        return transactionDAO.findAll().stream()
                .map(TransactionSummary::from)
                .toList();
    }

    public int applyMonthlyInterest() throws SQLException {
        loginController.requireRole("ADMIN");
        return transactionDAO.applyMonthlyInterest();
    }

    private AccountDAO.AccountRecord verifyTransactionOwner(String customerId,
                                                            String accountNumber)
            throws SQLException {
        AccountDAO.AccountRecord account = verifyAccountAccess(accountNumber);

        if (loginController.isTeller() && !account.customerId().equals(customerId)) {
            throw new SecurityException("Select the customer who owns this account");
        }

        return account;
    }

    private AccountDAO.AccountRecord verifyAccountAccess(String accountNumber)
            throws SQLException {
        loginController.requireRole("CUSTOMER", "TELLER");

        AccountDAO.AccountRecord account = accountDAO.findByAccountNumber(accountNumber);

        if (account == null) {
            throw new IllegalArgumentException("Account not found");
        }

        loginController.requireAccountAccess(account.customerId());
        return account;
    }
}