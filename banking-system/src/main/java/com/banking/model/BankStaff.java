
package com.banking.model;

import java.util.UUID;

public class BankStaff extends User {

    private final String staffId;
    private String position;
    private String branch;
    private Bank bank;

    public BankStaff(
            String userId,
            String username,
            String passwordHash,
            String staffId,
            String position,
            String branch) {

        super(userId, username, passwordHash);

        if (staffId == null || staffId.isBlank()) {
            throw new IllegalArgumentException(
                "Staff ID is required."
            );
        }

        if (position == null || position.isBlank()) {
            throw new IllegalArgumentException(
                "Position is required."
            );
        }

        if (branch == null || branch.isBlank()) {
            throw new IllegalArgumentException(
                "Branch is required."
            );
        }

        this.staffId = staffId;
        this.position = position;
        this.branch = branch;
    }

    public BankStaff(
            String userId,
            String username,
            String passwordHash,
            String staffId,
            String position,
            String branch,
            Bank bank) {

        this(userId, username, passwordHash,
             staffId, position, branch);

        assignBank(bank);
    }

    public String getStaffId() {
        return staffId;
    }

    public String getPosition() {
        return position;
    }

    public String getBranch() {
        return branch;
    }

    public Bank getBank() {
        return bank;
    }

    @Override
    public String getRole() {
        return "Bank Staff";
    }

    public void updatePosition(String newPosition) {

        if (newPosition == null || newPosition.isBlank()) {
            throw new IllegalArgumentException(
                "Position cannot be empty."
            );
        }

        this.position = newPosition;
    }

    public void updateBranch(String newBranch) {

        if (newBranch == null || newBranch.isBlank()) {
            throw new IllegalArgumentException(
                "Branch cannot be empty."
            );
        }

        this.branch = newBranch;
    }

    public void assignBank(Bank bank) {

        if (bank == null) {
            throw new IllegalArgumentException(
                "Bank cannot be null."
            );
        }

        this.bank = bank;
    }

    public boolean canPerformBankingOperations() {
        return isActive() && bank != null;
    }

    private void checkStaffAccess() {

        if (!isActive()) {
            throw new IllegalStateException(
                "Staff account is inactive."
            );
        }

        if (bank == null) {
            throw new IllegalStateException(
                "Staff must be assigned to a bank."
            );
        }
    }

    public void registerCustomer(Customer customer) {

        checkStaffAccess();

        bank.registerCustomer(customer, this);
    }

    private void checkRegisteredCustomer(Customer customer) {

        checkStaffAccess();

        if (customer == null) {
            throw new IllegalArgumentException(
                "Customer cannot be null."
            );
        }

        if (bank.findCustomer(customer.getCustomerId())
                != customer) {

            throw new IllegalArgumentException(
                "Customer must be registered first."
            );
        }
    }

    private <T extends Account> T registerNewAccount(
            T account) {

        checkRegisteredCustomer(account.getOwner());

        bank.addAccount(account, this);

        bank.recordTransaction(new Transaction(
                "ACCOUNT_OPENING",
                account.getBalance(),
                account,
                "Opened by staff " + staffId
        ));

        return account;
    }

    public SavingsAccount openSavingsAccount(
            Customer customer,
            double initialDeposit) {

        checkRegisteredCustomer(customer);

        SavingsAccount account = new SavingsAccount(
                "SAV-" + UUID.randomUUID(),
                initialDeposit,
                bank.getBranchName(),
                customer
        );

        return registerNewAccount(account);
    }

    public InvestmentAccount openInvestmentAccount(
            Customer customer,
            double initialDeposit) {

        checkRegisteredCustomer(customer);

        InvestmentAccount account = new InvestmentAccount(
                "INV-" + UUID.randomUUID(),
                initialDeposit,
                bank.getBranchName(),
                customer
        );

        return registerNewAccount(account);
    }

    public ChequeAccount openChequeAccount(
            IndividualCustomer customer,
            double initialDeposit) {

        checkRegisteredCustomer(customer);

        ChequeAccount account = new ChequeAccount(
                "CHQ-" + UUID.randomUUID(),
                initialDeposit,
                bank.getBranchName(),
                customer
        );

        return registerNewAccount(account);
    }

    public boolean processDeposit(
            String accountNumber,
            double amount,
            String reference) {

        checkStaffAccess();

        Account account = bank.findAccount(accountNumber);

        if (account == null) {
            return false;
        }

        if (!account.deposit(amount, reference)) {
            return false;
        }

        bank.recordTransaction(new Transaction(
                "DEPOSIT",
                amount,
                account,
                reference
        ));

        return true;
    }

    public boolean processDeposit(
            String accountNumber,
            double amount) {

        return processDeposit(accountNumber, amount, "");
    }

    public boolean processWithdrawal(
            String accountNumber,
            double amount) {

        checkStaffAccess();

        Account account = bank.findAccount(accountNumber);

        if (!(account instanceof Withdrawable withdrawable)) {
            return false;
        }

        if (!withdrawable.withdraw(amount)) {
            return false;
        }

        bank.recordTransaction(new Transaction(
                "WITHDRAWAL",
                amount,
                account,
                "Processed by staff " + staffId
        ));

        return true;
    }
}
