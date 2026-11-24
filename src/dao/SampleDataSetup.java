package dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Locale;

public class SampleDataSetup {

    public static final String ADMIN_USERNAME = "Reuben Fani";
    public static final String TELLER_USERNAME = "Tsotlhe Mokakangwe";
    public static final String PASSWORD = "BankDemo2026";

    private static final String[] CELL_NUMBERS = {
            "72885637", "75990278", "72188642", "79260552", "76522907",
            "72638901", "71727910", "78925192", "78924624", "75178920"
    };

    private static final String[] NAMES = {
            "Unoda chikuba",
            "Lefika Modisa",
            "Dithapelo Ramosarwa",
            "Katlego Mokoya",
            "Uendjipa Kajuu",
            "Alex Tlhako",
            "Sadi Ditshego",
            "Jessica Vero",
            "Fiona Gobe",
            "Segametsi Bolele"
    };

    public static void initializeSampleUsers() throws SQLException {
        String markerSql = "INSERT OR IGNORE INTO demo_setup (id) VALUES (1)";

        try (Connection connection = DatabaseManager.getConnection()) {
            connection.setAutoCommit(false);

            try (PreparedStatement marker = connection.prepareStatement(markerSql)) {
                if (marker.executeUpdate() == 1) {
                    seed(connection);
                }
                connection.commit();
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            }
        }
    }

    private static void seed(Connection connection) throws SQLException {
        UserDAO users = new UserDAO();
        TransactionDAO transactions = new TransactionDAO();

        users.createUser(connection, ADMIN_USERNAME, PASSWORD, "ADMIN");
        String tellerId = users.createUser(
                connection, TELLER_USERNAME, PASSWORD, "TELLER");

        LocalDateTime start = LocalDateTime.now(MonthlyInterestScheduler.BANK_ZONE)
                .minusDays(1).withSecond(0).withNano(0);

        for (int index = 0; index < NAMES.length; index++) {
            int sequence = 101 + index;
            String customerId = "CUS-" + sequence;
            String accountNumber = "100000" + sequence;
            String userId = users.createUser(
                    connection, NAMES[index], PASSWORD, "CUSTOMER");

            String simpleUserId = String.format(Locale.ROOT, "U%02d", index + 1);
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE users SET user_id = ? WHERE user_id = ?")) {
                statement.setString(1, simpleUserId);
                statement.setString(2, userId);
                if (statement.executeUpdate() != 1) {
                    throw new SQLException("Could not assign customer user ID");
                }
            }
            insertCustomer(connection, index, customerId, simpleUserId);

            String accountType = switch (index % 3) {
                case 0 -> "SAVINGS";
                case 1 -> "INVESTMENT";
                default -> "CHEQUE";
            };

            if ("CHEQUE".equals(accountType) || "CUS-102".equals(customerId)) {
                insertEmployment(connection, customerId);
            }

            LocalDateTime openedAt = start.plusMinutes(index * 10L);
            BigDecimal openingDeposit = BigDecimal.valueOf(1000 + index * 100);
            openFundedAccount(connection, transactions, customerId, accountNumber,
                    accountType, openingDeposit, tellerId, openedAt);

            transactions.postTransaction(connection, accountNumber,
                    new BigDecimal("100.00"), tellerId, false, openedAt.plusMinutes(2));

            if (!"SAVINGS".equals(accountType)) {
                transactions.postTransaction(connection, accountNumber,
                        new BigDecimal("40.00"), tellerId, true, openedAt.plusMinutes(4));
            }
        }

        // Give Lefika Modisa all three account types for the presentation.
        openFundedAccount(connection, transactions, "CUS-102", "100001102",
                "SAVINGS", new BigDecimal("250.00"), tellerId, start.plusMinutes(100));
        openFundedAccount(connection, transactions, "CUS-102", "100002102",
                "CHEQUE", new BigDecimal("250.00"), tellerId, start.plusMinutes(102));
    }

    private static void insertCustomer(Connection connection, int index,
                                       String customerId, String userId)
            throws SQLException {
        String[] name = NAMES[index].split(" ", 2);
        String sql = """
                INSERT INTO customers
                    (customer_id, customer_type, first_name, surname, gender,
                     address, identity_number, user_id, email, cell_no)
                VALUES (?, 'INDIVIDUAL', ?, ?, ?, 'Gaborone', ?, ?, ?, ?)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, customerId);
            statement.setString(2, name[0]);
            statement.setString(3, name[1]);
            statement.setString(4, index < 6 ? "Male" : "Female");
            statement.setString(5, customerId);
            statement.setString(6, userId);
            statement.setString(7, (name[1] + name[0]).toLowerCase(Locale.ROOT) + "@email.com");
            statement.setString(8, CELL_NUMBERS[index]);
            statement.executeUpdate();
        }
    }

    private static void insertEmployment(Connection connection, String customerId)
            throws SQLException {
        String sql = """
                INSERT INTO employment_details
                    (customer_id, employer_name, employer_address, job_title, employed)
                VALUES (?, 'Gaborone Trading', 'Gaborone', 'Clerk', 1)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, customerId);
            statement.executeUpdate();
        }
    }

    private static void openFundedAccount(Connection connection,
                                          TransactionDAO transactions,
                                          String customerId,
                                          String accountNumber,
                                          String accountType,
                                          BigDecimal openingDeposit,
                                          String tellerId,
                                          LocalDateTime openedAt)
            throws SQLException {
        String sql = """
                INSERT INTO accounts
                    (account_number, customer_id, account_type, balance, branch)
                VALUES (?, ?, ?, 0, 'Gaborone Main')
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, accountNumber);
            statement.setString(2, customerId);
            statement.setString(3, accountType);
            statement.executeUpdate();
        }

        transactions.postTransaction(
                connection, accountNumber, openingDeposit, tellerId, false, openedAt);
    }
}
