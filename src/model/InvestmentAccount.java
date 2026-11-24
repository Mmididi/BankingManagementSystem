package model;

import java.math.BigDecimal;

public class InvestmentAccount extends Account implements InterestBearing, Withdrawable {

    public InvestmentAccount(String accountNumber, double initialDeposit,
                             String branch, Customer owner) {
        super(accountNumber, initialDeposit, branch, owner);

        if (!validateInitialDeposit(initialDeposit)) {
            throw new IllegalArgumentException(
                    "Investment account requires a minimum initial deposit of BWP500");
        }
    }

    public boolean validateInitialDeposit(double amount) {
        return Double.isFinite(amount)
                && AccountRules.meetsInvestmentMinimum(BigDecimal.valueOf(amount));
    }

    @Override
    public boolean withdraw(double amount) {
        return debit(amount);
    }

    @Override
    public double calculateInterest() {
        return AccountRules.monthlyInterest(getAccountType(), BigDecimal.valueOf(getBalance()))
                .doubleValue();
    }

    @Override
    public void applyInterest() {
        double interest = calculateInterest();
        if (interest > 0) {
            boolean successful = deposit(interest);
            if (!successful) {
                throw new IllegalStateException("Unable to apply investment interest");
            }
        }
    }

    @Override
    public String getAccountType() {
        return "Investment";
    }
}