package controller;

import dao.CustomerAccessDAO;
import dao.CustomerDAO;
import dao.UserDAO;
import model.Customer;
import model.EmployeeDetails;
import model.IndividualCustomer;
import model.OrganisationCustomer;

import java.sql.SQLException;
import java.util.List;

public class CustomerController {

    private final LoginController loginController;
    private final CustomerDAO customerDAO = new CustomerDAO();
    private final CustomerAccessDAO customerAccessDAO = new CustomerAccessDAO();
    private final UserDAO userDAO = new UserDAO();

    public CustomerController(LoginController loginController) {
        if (loginController == null) {
            throw new IllegalArgumentException("Login controller is required");
        }
        this.loginController = loginController;
    }

    public String registerIndividual(String firstName, String surname, String address,
                                     String gender, String email, String cellNo,
                                     String identityNumber) throws SQLException {
        loginController.requireRole("TELLER");

        String customerId = customerDAO.generateCustomerId();
        IndividualCustomer customer = new IndividualCustomer(
                customerId, firstName, surname, address, gender, email, cellNo,
                identityNumber
        );

        customerDAO.registerCustomer(customer);
        return customerId;
    }

    public String registerOrganisation(String organisationName, String registrationNumber,
                                       String businessAddress) throws SQLException {
        loginController.requireRole("TELLER");

        String customerId = customerDAO.generateCustomerId();
        OrganisationCustomer customer = new OrganisationCustomer(
                customerId, organisationName, registrationNumber, businessAddress
        );

        customerDAO.registerCustomer(customer);
        return customerId;
    }

    public void updateEmploymentDetails(String customerId, String employerName,
                                        String employerAddress, String jobTitle,
                                        boolean employed) throws SQLException {
        loginController.requireRole("TELLER");

        EmployeeDetails details = new EmployeeDetails(
                employerName, employerAddress, jobTitle, employed
        );

        customerDAO.saveEmploymentDetails(customerId, details);
    }

    public String createCustomerLogin(String customerId, String username, String password)
            throws SQLException {
        loginController.requireRole("TELLER");

        if (customerDAO.findById(customerId) == null) {
            throw new IllegalArgumentException("Customer not found");
        }

        if (customerAccessDAO.hasLogin(customerId)) {
            throw new IllegalArgumentException("Customer already has a login");
        }

        return customerAccessDAO.createLogin(customerId, username, password, userDAO);
    }

    public Customer findCustomer(String customerId) throws SQLException {
        loginController.requireRole("CUSTOMER", "TELLER", "ADMIN");

        if (loginController.isCustomer()
                && !loginController.getCurrentCustomerId().equals(customerId)) {
            throw new SecurityException("You cannot view another customer's details");
        }

        return customerDAO.findById(customerId);
    }

    public record CustomerSummary(String id, String name) {
        @Override
        public String toString() {
            return name + " (" + id + ")";
        }
    }

    public List<CustomerSummary> getAllCustomers() throws SQLException {
        loginController.requireRole("TELLER", "ADMIN");

        return customerDAO.findAll().stream()
                .map(customer -> new CustomerSummary(
                        customer.getCustomerId(), customer.getCustomerName()))
                .toList();
    }

    public boolean updateContactDetails(String customerId, String address, String email,
                                        String cellNo) throws SQLException {
        loginController.requireRole("TELLER");
        return customerDAO.updateIndividualContact(customerId, address, email, cellNo);
    }
}