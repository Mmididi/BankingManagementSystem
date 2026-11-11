
package com.banking.model;

public class SavingsAccount extends Account
        implements InterestBearing {

    private static final double MONTHLY_RATE = 0.0005;

    public SavingsAccount(
            String accountNumber,
            double balance,
            String branch,
            Customer owner) {

        super(accountNumber, balance, branch, owner);
    }

    public double getMonthlyRate() {
        return MONTHLY_RATE;
    }

    @Override
    public double calculateInterest() {
        return getBalance() * MONTHLY_RATE;
    }

    @Override
    public void applyInterest() {

        double interest = calculateInterest();

        double newBalance = getBalance() + interest;

        if (!Double.isFinite(newBalance)) {
            throw new IllegalStateException(
                "Interest calculation produced an invalid balance."
            );
        }

        setBalance(newBalance);
    }

    @Override
    public String getAccountType() {
        return "Savings";
    }
}
