
package com.banking;

import com.banking.model.*;
import junit.framework.TestCase;

public class BankingCoreTest extends TestCase {

    private Bank bank;
    private BankStaff staff;
    private IndividualCustomer individual;
    private OrganisationCustomer organisation;

    @Override
    protected void setUp() {

        bank = new Bank(
                "B01",
                "Test Bank",
                "Main Branch"
        );

        staff = new BankStaff(
                "U01",
                "teststaff",
                "test-password-hash",
                "S01",
                "Teller",
                "Main Branch",
                bank
        );

        individual = new IndividualCustomer(
                "C01",
                "Test",
                "Customer",
                "Gaborone",
                "Male",
                "test@example.com",
                "70000000",
                "123456789"
        );

        organisation = new OrganisationCustomer(
                "C02",
                "Test Organisation",
                "REG123",
                "Gaborone"
        );
    }

    public void testCustomerRegistration() {

        staff.registerCustomer(individual);
        staff.registerCustomer(organisation);

        assertEquals(2, bank.getTotalCustomers());

        assertSame(individual,
                bank.findCustomer("C01"));

        assertSame(organisation,
                bank.findCustomer("C02"));
    }

    public void testMultipleAccounts() {

        staff.registerCustomer(individual);

        individual.updateEmploymentDetails(
                new EmploymentDetails(
                        "Test Company",
                        "Gaborone",
                        "Manager",
                        true
                )
        );

        SavingsAccount savings =
                staff.openSavingsAccount(individual, 1000);

        InvestmentAccount investment =
                staff.openInvestmentAccount(individual, 500);

        ChequeAccount cheque =
                staff.openChequeAccount(individual, 200);

        assertEquals(3, bank.getTotalAccounts());

        assertEquals(3,
                bank.getCustomerAccounts("C01").size());

        assertSame(individual, savings.getOwner());
        assertSame(individual, investment.getOwner());
        assertSame(individual, cheque.getOwner());
    }

    public void testSavingsInterestAndNoWithdrawal() {

        SavingsAccount savings =
                new SavingsAccount(
                        "S01",
                        10000,
                        "Main Branch",
                        individual
                );

        assertEquals(
                5.00,
                savings.calculateInterest(),
                0.001
        );

        savings.applyInterest();

        assertEquals(
                10005.00,
                savings.getBalance(),
                0.001
        );

        assertFalse(
                Withdrawable.class.isInstance(savings)
        );
    }

    public void testInvestmentRules() {

        try {

            new InvestmentAccount(
                    "I01",
                    400,
                    "Main Branch",
                    individual
            );

            fail("Opening deposit below BWP500 must fail.");

        } catch (IllegalArgumentException expected) {
            // Correct: minimum deposit enforced.
        }

        InvestmentAccount investment =
                new InvestmentAccount(
                        "I02",
                        500,
                        "Main Branch",
                        individual
                );

        assertFalse(investment.withdraw(600));

        assertTrue(investment.withdraw(200));

        assertEquals(
                300.00,
                investment.getBalance(),
                0.001
        );

        assertEquals(
                15.00,
                investment.calculateInterest(),
                0.001
        );

        investment.applyInterest();

        assertEquals(
                315.00,
                investment.getBalance(),
                0.001
        );
    }

    public void testChequeAccountEmployment() {

        try {

            new ChequeAccount(
                    "CH01",
                    200,
                    "Main Branch",
                    individual
            );

            fail("Unemployed customer must be rejected.");

        } catch (IllegalArgumentException expected) {
            // Correct: employment is required.
        }

        individual.updateEmploymentDetails(
                new EmploymentDetails(
                        "Test Company",
                        "Gaborone",
                        "Manager",
                        true
                )
        );

        ChequeAccount cheque =
                new ChequeAccount(
                        "CH02",
                        200,
                        "Main Branch",
                        individual
                );

        assertTrue(cheque.withdraw(50));

        assertEquals(
                150.00,
                cheque.getBalance(),
                0.001
        );

        assertFalse(cheque.withdraw(500));
    }

    public void testTransactionHistory() {

        staff.registerCustomer(individual);

        SavingsAccount savings =
                staff.openSavingsAccount(individual, 200);

        assertTrue(
                staff.processDeposit(
                        savings.getAccountNumber(),
                        50,
                        "Cash deposit"
                )
        );

        assertEquals(
                250.00,
                savings.getBalance(),
                0.001
        );

        assertEquals(
                2,
                bank.getTransactionHistory(
                        savings.getAccountNumber()
                ).size()
        );

        Transaction transaction =
                bank.getTransactionHistory(
                        savings.getAccountNumber()
                ).get(1);

        assertEquals(
                "DEPOSIT",
                transaction.getTransactionType()
        );

        assertEquals(
                250.00,
                transaction.getBalanceAfter(),
                0.001
        );
    }

    public void testRejectUnregisteredCustomer() {

        try {

            staff.openSavingsAccount(individual, 500);

            fail("Unregistered customer must be rejected.");

        } catch (IllegalArgumentException expected) {
            // Correct: customer must register first.
        }
    }

    public void testInactiveStaffCannotRegister() {

        SystemAdministrator admin =
                new SystemAdministrator(
                        "U99",
                        "admin",
                        "admin-password-hash",
                        "A01",
                        "Administrator"
                );

        admin.deactivateUser(staff);

        assertFalse(staff.isActive());

        try {

            staff.registerCustomer(individual);

            fail("Inactive staff must not register customers.");

        } catch (IllegalStateException expected) {
            // Correct: inactive staff rejected.
        }
    }
}
