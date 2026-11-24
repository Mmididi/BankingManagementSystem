package model;

public class OrganisationCustomer extends Customer {
    private String organisationName;
    private String registrationNumber;
    private String businessAddress;

    public OrganisationCustomer(String customerId, String organisationName,
                                String registrationNumber, String businessAddress) {
        super(customerId);
        updateDetails(organisationName, registrationNumber, businessAddress);
    }

    public String getOrganisationName() { return organisationName; }
    public String getRegistrationNumber() { return registrationNumber; }
    public String getBusinessAddress() { return businessAddress; }

    @Override
    public String getCustomerName() {
        return organisationName;
    }

    public void updateDetails(String organisationName, String registrationNumber,
                              String businessAddress) {
        if (organisationName == null || organisationName.isBlank()) {
            throw new IllegalArgumentException("Organisation name is required");
        }
        if (registrationNumber == null || registrationNumber.isBlank()) {
            throw new IllegalArgumentException("Registration number is required");
        }
        if (businessAddress == null || businessAddress.isBlank()) {
            throw new IllegalArgumentException("Business address is required");
        }

        this.organisationName = organisationName;
        this.registrationNumber = registrationNumber;
        this.businessAddress = businessAddress;
    }
}