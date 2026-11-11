
package com.banking.model;

public abstract class Customer {

    private final String customerId;

    public Customer(String customerId) {

        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException(
                "Customer ID is required."
            );
        }

        this.customerId = customerId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public abstract String getCustomerName();
}
