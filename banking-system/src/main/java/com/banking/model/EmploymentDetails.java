
package com.banking.model;

public class EmploymentDetails {

    private String employerName;
    private String employerAddress;
    private String jobTitle;
    private boolean employed;

    public EmploymentDetails(
            String employerName,
            String employerAddress,
            String jobTitle,
            boolean employed) {

        updateDetails(
            employerName,
            employerAddress,
            jobTitle,
            employed
        );
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

    public boolean verifyEmployment() {

        return employed
                && employerName != null
                && !employerName.isBlank()
                && employerAddress != null
                && !employerAddress.isBlank();
    }

    public void updateDetails(
            String employerName,
            String employerAddress,
            String jobTitle,
            boolean employed) {

        if (employed) {

            if (employerName == null
                    || employerName.isBlank()) {

                throw new IllegalArgumentException(
                    "Employer name is required."
                );
            }

            if (employerAddress == null
                    || employerAddress.isBlank()) {

                throw new IllegalArgumentException(
                    "Employer address is required."
                );
            }
        }

        this.employerName = employerName;
        this.employerAddress = employerAddress;
        this.jobTitle = jobTitle;
        this.employed = employed;
    }
}
