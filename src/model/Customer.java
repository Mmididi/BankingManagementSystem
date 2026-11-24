package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public abstract class Customer {
    private final String customerId;
    private final List<Account> accounts;

    public Customer(String customerId) {
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("Customer ID cannot be empty");
        }
        this.customerId = customerId;
        this.accounts = new ArrayList<>();
    }

    public String getCustomerId() {
        return customerId;
    }

    public void addAccount(Account account) {
        Objects.requireNonNull(account, "Account is required");

        if (account.getOwner() != this) {
            throw new IllegalArgumentException("Account belongs to another customer");
        }
        if (!accounts.contains(account)) {
            accounts.add(account);
        }
    }

    public List<Account> getAccounts() {
        return Collections.unmodifiableList(accounts);
    }

    public abstract String getCustomerName();
}