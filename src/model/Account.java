package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public abstract class Account {
    private final String accountNumber;
    private double balance;
    private final String branch;
    private final Customer owner;
    private final List<Transaction> transactions;

    public Account(String accountNumber, double balance, String branch, Customer owner) {
        if (accountNumber == null || accountNumber.isBlank()) {
            throw new IllegalArgumentException("Account number is required");
        }
        if (branch == null || branch.isBlank()) {
            throw new IllegalArgumentException("Branch is required");
        }
        if (owner == null) {
            throw new IllegalArgumentException("An account must have a customer");
        }
        if (!Double.isFinite(balance) || balance < 0) {
            throw new IllegalArgumentException("Invalid opening balance");
        }

        this.accountNumber = accountNumber;
        this.balance = balance;
        this.branch = branch;
        this.owner = owner;
        this.transactions = new ArrayList<>();
    }

    public boolean deposit(double amount) {
        if (!Double.isFinite(amount) || amount <= 0) {
            return false;
        }

        double newBalance = balance + amount;
        if (!Double.isFinite(newBalance)) {
            return false;
        }

        balance = newBalance;
        return true;
    }

    public boolean deposit(double amount, String reference) {
        if (reference == null || reference.isBlank()) {
            return false;
        }
        return deposit(amount);
    }

    protected boolean debit(double amount) {
        if (!Double.isFinite(amount) || amount <= 0 || amount > balance) {
            return false;
        }
        balance -= amount;
        return true;
    }

    public void recordTransaction(String transactionType, double amount, BankStaff createdBy) {
        String transactionId = UUID.randomUUID().toString();
        Transaction transaction = new Transaction(
                transactionId, transactionType, amount, this, createdBy);
        transactions.add(transaction);
    }

    public List<Transaction> getTransactionHistory() {
        return Collections.unmodifiableList(transactions);
    }

    public double getBalance() {
        return balance;
    }

    public Customer getOwner() {
        return owner;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getBranch() {
        return branch;
    }

    public abstract String getAccountType();
}