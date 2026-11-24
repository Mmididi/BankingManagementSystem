package dao;

import model.Customer;
import model.EmployeeDetails;
import model.IndividualCustomer;
import model.OrganisationCustomer;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CustomerDAO {

    private static final String INSERT_EMPLOYMENT = """
            INSERT INTO employment_details
                (customer_id, employer_name, employer_address, job_title, employed)
            VALUES (?, ?, ?, ?, ?)
            """;

    public String generateCustomerId() throws SQLException {
        String sql = "INSERT INTO customer_id_sequence DEFAULT VALUES RETURNING id";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {

            if (!result.next()) {
                throw new SQLException("Could not generate a customer ID.");
            }

            return String.format(Locale.ROOT, "CUS%02d", result.getLong("id"));
        }
    }

    public void registerCustomer(Customer customer) throws SQLException {
        registerCustomer(customer, null);
    }

    public void registerCustomer(Customer customer, String userId)
            throws SQLException {
        if (customer == null) {
            throw new IllegalArgumentException("Customer is required");
        }

        try (Connection connection = DatabaseManager.getConnection()) {
            connection.setAutoCommit(false);

            try {
                if (customer instanceof IndividualCustomer individual) {
                    insertIndividual(connection, individual, userId);
                } else if (customer instanceof OrganisationCustomer organisation) {
                    insertOrganisation(connection, organisation, userId);
                } else {
                    throw new IllegalArgumentException("Unsupported customer type");
                }

                connection.commit();
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            }
        }
    }

    private void insertIndividual(Connection connection, IndividualCustomer customer,
                                  String userId) throws SQLException {
        String sql = """
                INSERT INTO customers
                    (customer_id, customer_type, first_name, surname,
                     address, gender, email, cell_no, identity_number, user_id)
                VALUES (?, 'INDIVIDUAL', ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, customer.getCustomerId());
            statement.setString(2, customer.getFirstName());
            statement.setString(3, customer.getSurname());
            statement.setString(4, customer.getAddress());
            statement.setString(5, customer.getGender());
            statement.setString(6, customer.getEmail());
            statement.setString(7, customer.getCellNo());
            statement.setString(8, customer.getIdentityNumber());
            statement.setString(9, userId);
            statement.executeUpdate();
        }

        EmployeeDetails details = customer.getEmploymentDetails();
        if (details != null) {
            writeEmployment(connection, INSERT_EMPLOYMENT,
                    customer.getCustomerId(), details);
        }
    }

    private void insertOrganisation(Connection connection,
                                    OrganisationCustomer customer, String userId)
            throws SQLException {
        String sql = """
                INSERT INTO customers
                    (customer_id, customer_type, organisation_name,
                     registration_number, business_address, user_id)
                VALUES (?, 'ORGANISATION', ?, ?, ?, ?)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, customer.getCustomerId());
            statement.setString(2, customer.getOrganisationName());
            statement.setString(3, customer.getRegistrationNumber());
            statement.setString(4, customer.getBusinessAddress());
            statement.setString(5, userId);
            statement.executeUpdate();
        }
    }

    public void saveEmploymentDetails(String customerId, EmployeeDetails details)
            throws SQLException {
        if (details == null) {
            throw new IllegalArgumentException("Employment details are required");
        }

        String check = "SELECT customer_type FROM customers WHERE customer_id = ?";
        String sql = INSERT_EMPLOYMENT + """
                ON CONFLICT(customer_id) DO UPDATE SET
                    employer_name = excluded.employer_name,
                    employer_address = excluded.employer_address,
                    job_title = excluded.job_title,
                    employed = excluded.employed
                """;

        try (Connection connection = DatabaseManager.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(check)) {
                statement.setString(1, customerId);

                try (ResultSet result = statement.executeQuery()) {
                    if (!result.next()
                            || !"INDIVIDUAL".equals(result.getString("customer_type"))) {
                        throw new IllegalArgumentException(
                                "An individual customer is required");
                    }
                }
            }

            writeEmployment(connection, sql, customerId, details);
        }
    }

    private void writeEmployment(Connection connection, String sql, String customerId,
                                 EmployeeDetails details) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, customerId);
            statement.setString(2, details.getEmployerName());
            statement.setString(3, details.getEmployerAddress());
            statement.setString(4, details.getJobTitle());
            statement.setInt(5, details.isEmployed() ? 1 : 0);
            statement.executeUpdate();
        }
    }

    public Customer findById(String customerId) throws SQLException {
        String sql = "SELECT * FROM customers WHERE customer_id = ?";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, customerId);

            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? mapCustomer(connection, result) : null;
            }
        }
    }

    public List<Customer> findAll() throws SQLException {
        String sql = "SELECT * FROM customers ORDER BY customer_id";
        List<Customer> customers = new ArrayList<>();

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {

            while (result.next()) {
                customers.add(mapCustomer(connection, result));
            }
        }

        return customers;
    }

    private Customer mapCustomer(Connection connection, ResultSet result)
            throws SQLException {
        String customerId = result.getString("customer_id");
        String customerType = result.getString("customer_type");

        if ("INDIVIDUAL".equals(customerType)) {
            IndividualCustomer customer = new IndividualCustomer(
                    customerId,
                    result.getString("first_name"),
                    result.getString("surname"),
                    result.getString("address"),
                    result.getString("gender"),
                    result.getString("email"),
                    result.getString("cell_no"),
                    result.getString("identity_number")
            );

            String sql = "SELECT * FROM employment_details WHERE customer_id = ?";

            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, customerId);

                try (ResultSet employment = statement.executeQuery()) {
                    if (employment.next()) {
                        EmployeeDetails details = new EmployeeDetails(
                                employment.getString("employer_name"),
                                employment.getString("employer_address"),
                                employment.getString("job_title"),
                                employment.getInt("employed") == 1
                        );
                        customer.updateEmploymentDetails(details);
                    }
                }
            }

            return customer;
        }

        if ("ORGANISATION".equals(customerType)) {
            return new OrganisationCustomer(
                    customerId,
                    result.getString("organisation_name"),
                    result.getString("registration_number"),
                    result.getString("business_address")
            );
        }

        throw new SQLException("Unknown customer type: " + customerType);
    }

    public boolean deleteCustomer(String customerId) throws SQLException {
        String sql = "DELETE FROM customers WHERE customer_id = ?";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, customerId);
            return statement.executeUpdate() == 1;
        }
    }

    public boolean updateIndividualContact(String customerId, String address,
                                           String email, String cellNo)
            throws SQLException {
        String sql = """
                UPDATE customers
                SET address = ?, email = ?, cell_no = ?
                WHERE customer_id = ?
                AND customer_type = 'INDIVIDUAL'
                """;

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, address);
            statement.setString(2, email);
            statement.setString(3, cellNo);
            statement.setString(4, customerId);
            return statement.executeUpdate() == 1;
        }
    }
}