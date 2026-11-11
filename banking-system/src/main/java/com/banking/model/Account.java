
package com.banking.model;

public abstract class Account {

    private final String accountNumber;
    private double balance;
    private String branch;
    private final Customer owner;

    public Account(
            String accountNumber,
            double balance,
            String branch,
            Customer owner) {

        if (accountNumber == null ||
                accountNumber.isBlank()) {
            throw new IllegalArgumentException(
                "Account number is required."
            );
        }

        if (!Double.isFinite(balance) || balance < 0) {
            throw new IllegalArgumentException(
                "Invalid opening balance."
            );
        }

        if (branch == null || branch.isBlank()) {
            throw new IllegalArgumentException(
                "Branch is required."
            );
        }

        if (owner == null) {
            throw new IllegalArgumentException(
                "An account must belong to a customer."
            );
        }

        this.accountNumber = accountNumber;
        this.balance = balance;
        this.branch = branch;
        this.owner = owner;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public double getBalance() {
        return balance;
    }

    public String getBranch() {
        return branch;
    }

    public Customer getOwner() {
        return owner;
    }

    public void updateBranch(String newBranch) {

        if (newBranch == null || newBranch.isBlank()) {
            throw new IllegalArgumentException(
                "Branch cannot be empty."
            );
        }

        this.branch = newBranch;
    }

    protected void setBalance(double newBalance) {

        if (!Double.isFinite(newBalance) ||
                newBalance < 0) {
            throw new IllegalArgumentException(
                "Invalid account balance."
            );
        }

        this.balance = newBalance;
    }

    public boolean deposit(double amount) {

        if (!Double.isFinite(amount) || amount <= 0) {
            return false;
        }

        double newBalance = balance + amount;

        if (!Double.isFinite(newBalance)) {
            return false;
        }

        setBalance(newBalance);

        return true;
    }

    public boolean deposit(
            double amount,
            String reference) {

        return deposit(amount);
    }

    public abstract String getAccountType();
}
