package model;

public class IndividualCustomer extends Customer {
    private String firstName;
    private String surname;
    private String address;
    private String gender;
    private String email;
    private String cellNo;
    private String identityNumber;
    private EmployeeDetails employmentDetails;

    public IndividualCustomer(String customerId, String firstName, String surname,
                              String address, String gender, String email,
                              String cellNo, String identityNumber,
                              EmployeeDetails employmentDetails) {
        super(customerId);

        if (firstName == null || firstName.isBlank()
                || surname == null || surname.isBlank()) {
            throw new IllegalArgumentException("First name and surname are required");
        }
        if (identityNumber == null || identityNumber.isBlank()) {
            throw new IllegalArgumentException("Identity number is required");
        }

        this.firstName = firstName;
        this.surname = surname;
        this.gender = gender;
        this.identityNumber = identityNumber;
        this.employmentDetails = employmentDetails;
        updateContactDetails(address, email, cellNo);
    }

    public IndividualCustomer(String customerId, String firstName, String surname,
                              String address, String gender, String email,
                              String cellNo, String identityNumber) {
        this(customerId, firstName, surname, address, gender, email, cellNo, identityNumber, null);
    }

    public String getFirstName() { return firstName; }
    public String getSurname() { return surname; }
    public String getAddress() { return address; }
    public String getGender() { return gender; }
    public String getEmail() { return email; }
    public String getCellNo() { return cellNo; }
    public String getIdentityNumber() { return identityNumber; }

    @Override
    public String getCustomerName() {
        return firstName + " " + surname;
    }

    public String getFullName() {
        return firstName + " " + surname;
    }

    public void updateContactDetails(String address, String email, String cellNo) {
        if (address == null || address.isBlank()) {
            throw new IllegalArgumentException("Address is required");
        }
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }
        if (cellNo == null || cellNo.isBlank()) {
            throw new IllegalArgumentException("Cell number is required");
        }

        this.address = address;
        this.email = email;
        this.cellNo = cellNo;
    }

    public void updateEmploymentDetails(EmployeeDetails employmentDetails) {
        this.employmentDetails = employmentDetails;
    }

    public EmployeeDetails getEmploymentDetails() {
        return employmentDetails;
    }
}