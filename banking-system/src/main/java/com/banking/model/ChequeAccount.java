
package com.banking.model;

public class ChequeAccount extends Account
        implements Withdrawable {

    public ChequeAccount(
            String accountNumber,
            double balance,
            String branch,
            IndividualCustomer owner) {

        super(accountNumber, balance, branch, owner);

        if (!owner.isEligibleForChequeAccount()) {
            throw new IllegalArgumentException(
                "Cheque Accounts are only available " +
                "to employed customers with valid " +
                "employer details."
            );
        }
    }

    public EmploymentDetails getEmploymentDetails() {

        IndividualCustomer customer =
                (IndividualCustomer) getOwner();

        return customer.getEmploymentDetails();
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
    public String getAccountType() {
        return "Cheque";
    }
}
