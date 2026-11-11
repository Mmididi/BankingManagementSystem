
package com.banking.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class Transaction {

    private final String transactionId;
    private final String transactionType;
    private final double amount;
    private final LocalDateTime dateTime;
    private final Account account;
    private final double balanceAfter;
    private final String reference;

    public Transaction(
            String transactionType,
            double amount,
            Account account,
            String reference) {

        if (transactionType == null ||
                transactionType.isBlank()) {
            throw new IllegalArgumentException(
                "Transaction type is required."
            );
        }

        if (!Double.isFinite(amount) || amount < 0) {
            throw new IllegalArgumentException(
                "Invalid transaction amount."
            );
        }

        if (amount == 0 &&
                !transactionType.equals("ACCOUNT_OPENING")) {
            throw new IllegalArgumentException(
                "Transaction amount must be greater than zero."
            );
        }

        if (account == null) {
            throw new IllegalArgumentException(
                "Transaction must have an account."
            );
        }

        this.transactionId = UUID.randomUUID().toString();
        this.transactionType = transactionType;
        this.amount = amount;
        this.account = account;
        this.reference = reference == null ? "" : reference;
        this.dateTime = LocalDateTime.now();
        this.balanceAfter = account.getBalance();
    }

    public Transaction(
            String transactionType,
            double amount,
            Account account) {

        this(transactionType, amount, account, "");
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public double getAmount() {
        return amount;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public Account getAccount() {
        return account;
    }

    public double getBalanceAfter() {
        return balanceAfter;
    }

    public String getReference() {
        return reference;
    }

    public String getTransactionDetails() {

        return "Transaction ID: " + transactionId
                + "\nType: " + transactionType
                + "\nAmount: BWP " + amount
                + "\nAccount: " + account.getAccountNumber()
                + "\nDate: " + dateTime
                + "\nBalance After: BWP " + balanceAfter
                + "\nReference: " + reference;
    }

    @Override
    public String toString() {
        return transactionType
                + " | BWP " + amount
                + " | " + dateTime;
    }
}
