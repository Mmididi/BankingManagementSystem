package model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Bank {
    private final String bankId;
    private final String bankName;
    private final String branchName;
    private final List<Customer> customers;
    private final List<Account> accounts;

    public Bank(String bankId, String bankName, String branchName) {
        if (bankId == null || bankId.isBlank()) {
            throw new IllegalArgumentException("Bank ID is required");
        }
        if (bankName == null || bankName.isBlank()) {
            throw new IllegalArgumentException("Bank name is required");
        }
        if (branchName == null || branchName.isBlank()) {
            throw new IllegalArgumentException("Branch name is required");
        }

        this.bankId = bankId;
        this.bankName = bankName;
        this.branchName = branchName;
        this.customers = new ArrayList<>();
        this.accounts = new ArrayList<>();
    }

    public void addCustomer(Customer customer) {
        Objects.requireNonNull(customer, "Customer is required");

        for (Customer existing : customers) {
            if (existing.getCustomerId().equals(customer.getCustomerId())) {
                if (existing == customer) {
                    return;
                }
                throw new IllegalArgumentException("Customer ID already exists");
            }
        }
        customers.add(customer);
    }

    public void addAccount(Account account) {
        Objects.requireNonNull(account, "Account is required");

        if (!customers.contains(account.getOwner())) {
            throw new IllegalArgumentException("Account owner must be registered with the bank");
        }

        for (Account existing : accounts) {
            if (existing.getAccountNumber().equals(account.getAccountNumber())) {
                if (existing == account) {
                    return;
                }
                throw new IllegalArgumentException("Account number already exists");
            }
        }

        account.getOwner().addAccount(account);
        accounts.add(account);
    }

    public Customer findCustomer(String customerId) {
        if (customerId == null || customerId.isBlank()) {
            return null;
        }

        for (Customer customer : customers) {
            if (customer.getCustomerId().equals(customerId)) {
                return customer;
            }
        }
        return null;
    }

    public Account findAccount(String accountNumber) {
        if (accountNumber == null || accountNumber.isBlank()) {
            return null;
        }

        for (Account account : accounts) {
            if (account.getAccountNumber().equals(accountNumber)) {
                return account;
            }
        }
        return null;
    }

    public void applyMonthlyInterest() {
        for (Account account : accounts) {
            if (account instanceof InterestBearing) {
                InterestBearing interestAccount = (InterestBearing) account;
                double interest = interestAccount.calculateInterest();

                if (!Double.isFinite(interest) || interest < 0) {
                    throw new IllegalStateException("Invalid calculated interest");
                }

                if (interest > 0) {
                    interestAccount.applyInterest();
                    account.recordTransaction("INTEREST", interest, null);
                }
            }
        }
    }

    public String getBankId() {
        return bankId;
    }

    public String getBankName() {
        return bankName;
    }

    public String getBranchName() {
        return branchName;
    }
}