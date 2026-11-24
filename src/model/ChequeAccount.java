package model;

public class ChequeAccount extends Account implements Withdrawable {

    public ChequeAccount(String accountNumber, double balance, String branch, Customer owner) {
        super(accountNumber, balance, branch, owner);

        if (!verifyEmployment()) {
            throw new IllegalArgumentException(
                    "Cheque accounts require an employed individual "
                            + "with valid employer details");
        }
    }

    public boolean verifyEmployment() {
        if (!(getOwner() instanceof IndividualCustomer)) {
            return false;
        }

        IndividualCustomer customer = (IndividualCustomer) getOwner();
        EmployeeDetails details = customer.getEmploymentDetails();
        return details != null && details.verifyEmployment();
    }

    @Override
    public boolean withdraw(double amount) {
        return debit(amount);
    }

    @Override
    public String getAccountType() {
        return "Cheque";
    }
}