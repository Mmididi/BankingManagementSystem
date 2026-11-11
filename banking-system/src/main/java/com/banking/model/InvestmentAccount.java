
package com.banking.model;

public class InvestmentAccount extends Account
        implements InterestBearing, Withdrawable {

    private static final double MONTHLY_RATE = 0.05;
    private static final double MINIMUM_OPENING_DEPOSIT = 500.00;

    public InvestmentAccount(
            String accountNumber,
            double balance,
            String branch,
            Customer owner) {

        super(accountNumber, balance, branch, owner);

        if (balance < MINIMUM_OPENING_DEPOSIT) {
            throw new IllegalArgumentException(
                "Investment Account requires a minimum " +
                "opening deposit of BWP 500."
            );
        }
    }

    public double getMonthlyRate() {
        return MONTHLY_RATE;
    }

    public double getMinimumOpeningDeposit() {
        return MINIMUM_OPENING_DEPOSIT;
    }

    @Override
    public boolean withdraw(double amount) {

        if (!Double.isFinite(amount) || amount <= 0) {
            return false;
        }

        if (amount > getBalance()) {
            return false;
        }

        setBalance(getBalance() - amount);

        return true;
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
        return "Investment";
    }
}
