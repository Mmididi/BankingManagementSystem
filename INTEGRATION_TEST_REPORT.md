# Banking Management System Integration Test Report

## Scope

The integration test exercises the JavaFX sign-in form and teller transaction desk, the controllers, the shared account rules, the JDBC DAOs, and the SQLite schema. It also checks customer CRUD directly through CustomerDAO. It uses a fresh temporary SQLite database, loads the ten sample customers, and removes the temporary database when finished.

## Results

| Check | Expected and observed result | Status |
| --- | --- | --- |
| Sample setup | The database contains ten seeded customers. | Pass |
| Customer CRUD | A temporary customer receives CUS01. The saved record can be read, the contact update persists, and deleting the record restores the original count. | Pass |
| Staff sign-in | An incorrect password is rejected. Correct teller credentials open the teller workspace. | Pass |
| Savings policy | Selecting the customer's Savings Account disables Withdraw. | Pass |
| Teller deposit | Posting BWP 75.00 increases the saved account balance by BWP 75.00 and creates exactly one matching SQLite ledger entry. | Pass |
| Customer transaction history | Signing in using the customer's ID opens their dashboard, where the same saved deposit appears in the ledger. | Pass |

The test passed locally on Windows using JDK 21, JavaFX 21, and SQLite JDBC 3.50.3.0. The GUI checks fire actual JavaFX controls programmatically. This run did not cover the administrator GUI or Codespaces.

## Run in IntelliJ

1. Open the project with JDK 21, the JavaFX SDK and SQLite JDBC attached to its module. Keep the resources folder marked as Resources Root.
2. Open tests/testing/IntegrationTest.java and use the green Run button beside main. The tests folder should be marked as Test Sources Root.
3. In the IntegrationTest run configuration, use the same JavaFX module-path options as Launcher. For example, if the SDK is beside the project folder:

   ```text
   --module-path "../javafx-sdk-21.0.13.1/lib" --add-modules javafx.controls
   ```

   The test selects its own database. A short-lived JavaFX window is expected.

Expected final console line: `All integration checks passed.`
