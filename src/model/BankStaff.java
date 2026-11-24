package model;

import java.util.Objects;

public class BankStaff extends User {
    private final String staffId;
    private String position;
    private String branch;
    private final Bank bank;

    public BankStaff(String userId, String username, String password,
                     String staffId, String position, String branch, Bank bank) {
        super(userId, username, password);

        if (staffId == null || staffId.isBlank()) {
            throw new IllegalArgumentException("Staff ID is required");
        }
        if (position == null || position.isBlank()) {
            throw new IllegalArgumentException("Position is required");
        }
        if (branch == null || branch.isBlank()) {
            throw new IllegalArgumentException("Branch is required");
        }

        this.staffId = staffId;
        this.position = position;
        this.branch = branch;
        this.bank = Objects.requireNonNull(bank, "Bank is required");
    }

    public String getStaffId() {
        return staffId;
    }

    public void registerCustomer(Customer customer) {
        bank.addCustomer(customer);
    }

    public SavingsAccount openSavingsAccount(String customerId, String accountNumber) {
        Customer customer = getRegisteredCustomer(customerId);
        SavingsAccount account = new SavingsAccount(accountNumber, 0.0, branch, customer);
        bank.addAccount(account);
        return account;
    }

    public InvestmentAccount openInvestmentAccount(String customerId, String accountNumber,
                                                   double initialDeposit) {
        Customer customer = getRegisteredCustomer(customerId);
        InvestmentAccount account = new InvestmentAccount(
                accountNumber, initialDeposit, branch, customer);
        bank.addAccount(account);
        account.recordTransaction("INITIAL_DEPOSIT", initialDeposit, this);
        return account;
    }

    public ChequeAccount openChequeAccount(String customerId, String accountNumber) {
        Customer customer = getRegisteredCustomer(customerId);
        ChequeAccount account = new ChequeAccount(accountNumber, 0.0, branch, customer);
        bank.addAccount(account);
        return account;
    }

    public boolean processDeposit(String accountNumber, double amount) {
        Account account = bank.findAccount(accountNumber);
        if (account == null) {
            return false;
        }

        boolean successful = account.deposit(amount);
        if (successful) {
            account.recordTransaction("DEPOSIT", amount, this);
        }
        return successful;
    }

    public boolean processDeposit(String accountNumber, double amount, String reference) {
        Account account = bank.findAccount(accountNumber);
        if (account == null) {
            return false;
        }

        boolean successful = account.deposit(amount, reference);
        if (successful) {
            account.recordTransaction("DEPOSIT", amount, this);
        }
        return successful;
    }

    public boolean processWithdrawal(String accountNumber, double amount) {
        Account account = bank.findAccount(accountNumber);
        if (!(account instanceof Withdrawable)) {
            return false;
        }

        Withdrawable withdrawable = (Withdrawable) account;
        boolean successful = withdrawable.withdraw(amount);
        if (successful) {
            account.recordTransaction("WITHDRAWAL", amount, this);
        }
        return successful;
    }

    private Customer getRegisteredCustomer(String customerId) {
        Customer customer = bank.findCustomer(customerId);
        if (customer == null) {
            throw new IllegalArgumentException("Customer is not registered with the bank");
        }
        return customer;
    }
}