package model;

import java.math.BigDecimal;

public class SavingsAccount extends Account implements InterestBearing {

    public SavingsAccount(String accountNumber, double balance, String branch, Customer owner) {
        super(accountNumber, balance, branch, owner);
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
                throw new IllegalStateException("Unable to apply savings interest");
            }
        }
    }

    @Override
    public String getAccountType() {
        return "Savings";
    }
}