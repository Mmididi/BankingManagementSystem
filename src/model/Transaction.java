package model;

import java.time.LocalDateTime;
import java.util.Objects;

public class Transaction {
    private final String transactionId;
    private final LocalDateTime transactionDate;
    private final String transactionType;
    private final double amount;
    private final Account account;
    private final BankStaff createdBy;

    public Transaction(String transactionId, String transactionType, double amount,
                       Account account, BankStaff createdBy) {
        if (transactionId == null || transactionId.isBlank()) {
            throw new IllegalArgumentException("Transaction ID is required");
        }
        if (transactionType == null || transactionType.isBlank()) {
            throw new IllegalArgumentException("Transaction type is required");
        }
        if (!Double.isFinite(amount) || amount <= 0) {
            throw new IllegalArgumentException("Transaction amount must be greater than zero");
        }

        this.transactionId = transactionId;
        this.transactionDate = LocalDateTime.now();
        this.transactionType = transactionType;
        this.amount = amount;
        this.account = Objects.requireNonNull(account, "Account is required");
        this.createdBy = createdBy;
    }

    public String getTransactionDetails() {
        String processedBy;
        if (createdBy != null) {
            processedBy = createdBy.getStaffId();
        } else {
            processedBy = "System";
        }

        return "Transaction ID: " + transactionId
                + "\nDate: " + transactionDate
                + "\nType: " + transactionType
                + "\nAmount: BWP " + amount
                + "\nAccount Number: " + account.getAccountNumber()
                + "\nProcessed By: " + processedBy;
    }
}