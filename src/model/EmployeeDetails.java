package model;

public class EmployeeDetails {
    private String employerName;
    private String employerAddress;
    private String jobTitle;
    private boolean employed;

    public EmployeeDetails(String employerName, String employerAddress,
                           String jobTitle, boolean employed) {
        this.jobTitle = jobTitle;
        updateDetails(employerName, employerAddress, employed);
    }

    public boolean verifyEmployment() {
        return employed
                && employerName != null
                && !employerName.isBlank()
                && employerAddress != null
                && !employerAddress.isBlank();
    }

    public void updateDetails(String employerName, String employerAddress, boolean employed) {
        this.employerName = employerName;
        this.employerAddress = employerAddress;
        this.employed = employed;
    }

    public String getEmployerName() {
        return employerName;
    }

    public String getEmployerAddress() {
        return employerAddress;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public boolean isEmployed() {
        return employed;
    }
}