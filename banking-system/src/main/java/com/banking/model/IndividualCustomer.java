
package com.banking.model;

public class IndividualCustomer extends Customer {

    private String firstName;
    private String surname;
    private String address;
    private String gender;
    private String email;
    private String cellNo;
    private String identityNumber;
    private EmploymentDetails employmentDetails;

    public IndividualCustomer(
            String customerId,
            String firstName,
            String surname,
            String address,
            String gender,
            String email,
            String cellNo,
            String identityNumber) {

        super(customerId);

        if (firstName == null || firstName.isBlank()) {
            throw new IllegalArgumentException(
                "First name is required."
            );
        }

        if (surname == null || surname.isBlank()) {
            throw new IllegalArgumentException(
                "Surname is required."
            );
        }

        if (identityNumber == null ||
                identityNumber.isBlank()) {
            throw new IllegalArgumentException(
                "Identity number is required."
            );
        }

        this.firstName = firstName;
        this.surname = surname;
        this.address = address;
        this.gender = gender;
        this.email = email;
        this.cellNo = cellNo;
        this.identityNumber = identityNumber;
        this.employmentDetails = null;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getSurname() {
        return surname;
    }

    public String getAddress() {
        return address;
    }

    public String getGender() {
        return gender;
    }

    public String getEmail() {
        return email;
    }

    public String getCellNo() {
        return cellNo;
    }

    public String getIdentityNumber() {
        return identityNumber;
    }

    public EmploymentDetails getEmploymentDetails() {
        return employmentDetails;
    }

    @Override
    public String getCustomerName() {
        return firstName + " " + surname;
    }

    public void updateContactDetails(
            String address,
            String email,
            String cellNo) {

        if (address == null || address.isBlank()) {
            throw new IllegalArgumentException(
                "Address cannot be empty."
            );
        }

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException(
                "Email cannot be empty."
            );
        }

        if (cellNo == null || cellNo.isBlank()) {
            throw new IllegalArgumentException(
                "Cell number cannot be empty."
            );
        }

        this.address = address;
        this.email = email;
        this.cellNo = cellNo;
    }

    public void updateName(
            String firstName,
            String surname) {

        if (firstName == null || firstName.isBlank()
                || surname == null || surname.isBlank()) {

            throw new IllegalArgumentException(
                "Customer name cannot be empty."
            );
        }

        this.firstName = firstName;
        this.surname = surname;
    }

    public void updateEmploymentDetails(
            EmploymentDetails employmentDetails) {

        if (employmentDetails == null) {
            throw new IllegalArgumentException(
                "Employment details cannot be null."
            );
        }

        this.employmentDetails = employmentDetails;
    }

    public boolean isEligibleForChequeAccount() {

        return employmentDetails != null
                && employmentDetails.verifyEmployment();
    }
}
