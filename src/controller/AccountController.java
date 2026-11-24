package controller;

import dao.AccountDAO;
import dao.CustomerDAO;
import model.AccountRules;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

public class AccountController {

    private final LoginController loginController;
    private final AccountDAO accountDAO = new AccountDAO();
    private final CustomerDAO customerDAO = new CustomerDAO();

    public AccountController(LoginController loginController) {
        if (loginController == null) {
            throw new IllegalArgumentException("Login controller is required");
        }
        this.loginController = loginController;
    }

    public String openSavingsAccount(String customerId, String branch)
            throws SQLException {
        loginController.requireRole("TELLER");
        return accountDAO.openSavingsAccount(customerId, branch);
    }

    public String openInvestmentAccount(String customerId, String branch,
                                        BigDecimal initialDeposit) throws SQLException {
        loginController.requireRole("TELLER");
        return accountDAO.openInvestmentAccount(customerId, branch, initialDeposit);
    }

    public String openChequeAccount(String customerId, String branch)
            throws SQLException {
        loginController.requireRole("TELLER");
        return accountDAO.openChequeAccount(customerId, branch);
    }

    public record AccountSummary(String accountNumber, String customerId,
                                 String accountType, BigDecimal balance, String branch) {

        public String accountTypeName() {
            return switch (accountType) {
                case "SAVINGS" -> "Savings Account";
                case "INVESTMENT" -> "Investment Account";
                case "CHEQUE" -> "Cheque Account";
                default -> accountType;
            };
        }

        public boolean withdrawalAllowed() {
            return AccountRules.allowsWithdrawal(accountType);
        }

        public String interestRateLabel() {
            BigDecimal rate = AccountRules.monthlyRate(accountType);
            if (rate.signum() == 0) {
                return "No monthly interest";
            }
            return rate.movePointRight(2).stripTrailingZeros().toPlainString()
                    + "% monthly interest";
        }

        public BigDecimal projectedInterest() {
            return AccountRules.monthlyInterest(accountType, balance);
        }

        private static AccountSummary from(AccountDAO.AccountRecord account) {
            return new AccountSummary(
                    account.accountNumber(), account.customerId(), account.accountType(),
                    account.balance(), account.branch()
            );
        }
    }

    public AccountSummary getAccount(String accountNumber) throws SQLException {
        loginController.requireRole("CUSTOMER", "TELLER", "ADMIN");

        AccountDAO.AccountRecord account = accountDAO.findByAccountNumber(accountNumber);
        if (account == null) {
            throw new IllegalArgumentException("Account not found");
        }

        loginController.requireAccountAccess(account.customerId());
        return AccountSummary.from(account);
    }

    public List<AccountSummary> getCustomerAccounts(String customerId)
            throws SQLException {
        loginController.requireAccountAccess(customerId);

        return accountDAO.findByCustomerId(customerId).stream()
                .map(AccountSummary::from)
                .toList();
    }

    public List<AccountSummary> getMyAccounts() throws SQLException {
        String customerId = loginController.getCurrentCustomerId();

        return accountDAO.findByCustomerId(customerId).stream()
                .map(AccountSummary::from)
                .toList();
    }

    public BigDecimal getBalance(String accountNumber) throws SQLException {
        getAccount(accountNumber);
        return accountDAO.getBalance(accountNumber);
    }

    public List<AccountSummary> getAllAccounts() throws SQLException {
        loginController.requireRole("TELLER", "ADMIN");

        return accountDAO.findAll().stream()
                .map(AccountSummary::from)
                .toList();
    }

    public boolean customerExists(String customerId) throws SQLException {
        loginController.requireRole("TELLER", "ADMIN");
        return customerDAO.findById(customerId) != null;
    }
}