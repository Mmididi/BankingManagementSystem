
package com.banking.model;

public class OrganisationCustomer extends Customer {

    private String organisationName;
    private String registrationNumber;
    private String businessAddress;

    public OrganisationCustomer(
            String customerId,
            String organisationName,
            String registrationNumber,
            String businessAddress) {

        super(customerId);

        if (organisationName == null ||
                organisationName.isBlank()) {
            throw new IllegalArgumentException(
                "Organisation name is required."
            );
        }

        if (registrationNumber == null ||
                registrationNumber.isBlank()) {
            throw new IllegalArgumentException(
                "Registration number is required."
            );
        }

        if (businessAddress == null ||
                businessAddress.isBlank()) {
            throw new IllegalArgumentException(
                "Business address is required."
            );
        }

        this.organisationName = organisationName;
        this.registrationNumber = registrationNumber;
        this.businessAddress = businessAddress;
    }

    public String getOrganisationName() {
        return organisationName;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public String getBusinessAddress() {
        return businessAddress;
    }

    @Override
    public String getCustomerName() {
        return organisationName;
    }

    public void updateOrganisationName(
            String newName) {

        if (newName == null || newName.isBlank()) {
            throw new IllegalArgumentException(
                "Organisation name cannot be empty."
            );
        }

        this.organisationName = newName;
    }

    public void updateBusinessAddress(
            String newAddress) {

        if (newAddress == null ||
                newAddress.isBlank()) {
            throw new IllegalArgumentException(
                "Business address cannot be empty."
            );
        }

        this.businessAddress = newAddress;
    }

    public void updateRegistrationNumber(
            String newRegistrationNumber) {

        if (newRegistrationNumber == null ||
                newRegistrationNumber.isBlank()) {
            throw new IllegalArgumentException(
                "Registration number cannot be empty."
            );
        }

        this.registrationNumber = newRegistrationNumber;
    }
}
