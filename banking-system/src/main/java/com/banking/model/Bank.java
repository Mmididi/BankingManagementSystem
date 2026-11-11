
package com.banking.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Bank {

    private final String bankId;
    private String bankName;
    private String branchName;

    private final Map<String, Customer> customers;
    private final Map<String, Account> accounts;
    private final List<Transaction> transactions;

    public Bank(
            String bankId,
            String bankName,
            String branchName) {

        if (bankId == null || bankId.isBlank()) {
            throw new IllegalArgumentException(
                "Bank ID is required."
            );
        }

        if (bankName == null || bankName.isBlank()) {
            throw new IllegalArgumentException(
                "Bank name is required."
            );
        }

        if (branchName == null || branchName.isBlank()) {
            throw new IllegalArgumentException(
                "Branch name is required."
            );
        }

        this.bankId = bankId;
        this.bankName = bankName;
        this.branchName = branchName;

        this.customers = new LinkedHashMap<>();
        this.accounts = new LinkedHashMap<>();
        this.transactions = new ArrayList<>();
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

    public void updateBankName(String newName) {

        if (newName == null || newName.isBlank()) {
            throw new IllegalArgumentException(
                "Bank name cannot be empty."
            );
        }

        this.bankName = newName;
    }

    public void updateBranchName(String newBranch) {

        if (newBranch == null || newBranch.isBlank()) {
            throw new IllegalArgumentException(
                "Branch name cannot be empty."
            );
        }

        this.branchName = newBranch;
    }

    private void checkStaffAccess(BankStaff staff) {

        if (staff == null ||
                !staff.canPerformBankingOperations()) {
            throw new IllegalStateException(
                "Authorised Bank Staff is required."
            );
        }
    }

    public void registerCustomer(
            Customer customer,
            BankStaff staff) {

        checkStaffAccess(staff);

        if (customer == null) {
            throw new IllegalArgumentException(
                "Customer cannot be null."
            );
        }

        String customerId = customer.getCustomerId();

        if (customers.containsKey(customerId)) {
            throw new IllegalArgumentException(
                "Customer already registered."
            );
        }

        customers.put(customerId, customer);
    }

    public Customer findCustomer(String customerId) {

        if (customerId == null) {
            return null;
        }

        return customers.get(customerId);
    }

    public List<Customer> getAllCustomers() {
        return List.copyOf(customers.values());
    }

    public void addAccount(
            Account account,
            BankStaff staff) {

        checkStaffAccess(staff);

        if (account == null) {
            throw new IllegalArgumentException(
                "Account cannot be null."
            );
        }

        String customerId =
                account.getOwner().getCustomerId();

        if (!customers.containsKey(customerId)) {
            throw new IllegalArgumentException(
                "Customer must be registered first."
            );
        }

        String accountNumber =
                account.getAccountNumber();

        if (accounts.containsKey(accountNumber)) {
            throw new IllegalArgumentException(
                "Account number already exists."
            );
        }

        accounts.put(accountNumber, account);
    }

    public Account findAccount(String accountNumber) {

        if (accountNumber == null) {
            return null;
        }

        return accounts.get(accountNumber);
    }

    public List<Account> getAllAccounts() {
        return List.copyOf(accounts.values());
    }

    public List<Account> getCustomerAccounts(
            String customerId) {

        List<Account> result = new ArrayList<>();

        for (Account account : accounts.values()) {

            if (account.getOwner()
                    .getCustomerId().equals(customerId)) {

                result.add(account);
            }
        }

        return List.copyOf(result);
    }

    public void recordTransaction(
            Transaction transaction) {

        if (transaction == null) {
            throw new IllegalArgumentException(
                "Transaction cannot be null."
            );
        }

        String accountNumber =
                transaction.getAccount().getAccountNumber();

        if (accounts.get(accountNumber)
                != transaction.getAccount()) {
            throw new IllegalArgumentException(
                "Transaction account is not registered."
            );
        }

        transactions.add(transaction);
    }

    public List<Transaction> getAllTransactions() {
        return List.copyOf(transactions);
    }

    public List<Transaction> getTransactionHistory(
            String accountNumber) {

        List<Transaction> history = new ArrayList<>();

        for (Transaction transaction : transactions) {

            if (transaction.getAccount()
                    .getAccountNumber()
                    .equals(accountNumber)) {

                history.add(transaction);
            }
        }

        return List.copyOf(history);
    }

    public int getTotalCustomers() {
        return customers.size();
    }

    public int getTotalAccounts() {
        return accounts.size();
    }
}
