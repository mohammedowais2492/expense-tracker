
package com.expensetracker.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Set;
import java.util.HashSet;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Integration tests for {@link DatabaseMigrationManager} */
class DatabaseMigrationManagerTest {

    private static final Set<String> EXPECTED_TABLES = Set.of(
            "User",
            "ExpenseProfile",
            "Category",
            "FinancialTransaction");

    private static final Set<String> EXPECTED_CATEGORIES = Set.of(
            "Rent",
            "Car Loan Payment",
            "Utilities",
            "Subscriptions",
            "Restaurant",
            "Gas",
            "Cab",
            "Food Order",
            "Groceries",
            "Miscellaneous",
            "Insurance",
            "Car Maintenance",
            "Shopping",
            "Healthcare",
            "Entertainment",
            "Travel");

    @TempDir
    Path tempDir;

    @Test
    void throwsNullPointerExceptionWhenDataSourceIsNull() {
        assertThrows(
                NullPointerException.class,
                () -> new DatabaseMigrationManager(null));
    }

    @Test
    void successfullyCreatesTablesWhenMigratingFreshDatabase()
            throws IOException, SQLException {

        DataSource dataSource = createTestDataSource();
        DatabaseMigrationManager manager = new DatabaseMigrationManager(dataSource);

        manager.migrate();

        Set<String> actualTables = new HashSet<>();

        String sql = """
                SELECT name
                FROM sqlite_master
                WHERE type = 'table'
                """;

        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(sql)) {

            while (resultSet.next()) {
                actualTables.add(resultSet.getString("name"));
            }
        }

        assertTrue(actualTables.containsAll(EXPECTED_TABLES));
    }

    @Test
    void successfullyCreatesFlywaySchemaHistory()
            throws IOException, SQLException {

        DataSource dataSource = createTestDataSource();
        DatabaseMigrationManager manager = new DatabaseMigrationManager(dataSource);

        manager.migrate();

        String sql = """
                SELECT version, success
                FROM flyway_schema_history
                ORDER BY installed_rank
                """;

        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(sql)) {

            assertTrue(resultSet.next());
            assertEquals("001", resultSet.getString("version"));
            assertEquals(1, resultSet.getInt("success"));

            assertTrue(resultSet.next());
            assertEquals("002", resultSet.getString("version"));
            assertEquals(1, resultSet.getInt("success"));

            assertFalse(resultSet.next());
        }
    }

    @Test
    void successfullySeedsDefaultCategories()
            throws IOException, SQLException {

        DataSource dataSource = createTestDataSource();
        DatabaseMigrationManager manager = new DatabaseMigrationManager(dataSource);

        manager.migrate();

        Set<String> actualCategories = new HashSet<>();

        String sql = """
                SELECT Name, Type, IsActive
                FROM Category
                """;

        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(sql)) {

            while (resultSet.next()) {
                String name = resultSet.getString("Name");
                String type = resultSet.getString("Type");
                int isActive = resultSet.getInt("IsActive");

                assertEquals("EXPENSE", type);
                assertEquals(1, isActive);
                assertTrue(actualCategories.add(name),
                        "Duplicate category: " + name);
            }
        }

        assertEquals(EXPECTED_CATEGORIES, actualCategories);
    }

    @Test
    void doesNotReapplyMigrationsWhenInvokedMultipleTimes()
            throws IOException, SQLException {

        DataSource dataSource = createTestDataSource();
        DatabaseMigrationManager manager = new DatabaseMigrationManager(dataSource);

        manager.migrate();

        String insertSql = """
                INSERT INTO User (FirstName, LastName)
                VALUES (?, ?)
                """;

        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(insertSql)) {

            statement.setString(1, "Test");
            statement.setString(2, "User");
            assertEquals(1, statement.executeUpdate());
        }

        manager.migrate();

        String countUsersSql = "SELECT COUNT(*) FROM User";
        String countCategoriesSql = "SELECT COUNT(*) FROM Category";
        String countMigrationsSql = "SELECT COUNT(*) FROM flyway_schema_history";

        try (Connection connection = dataSource.getConnection()) {
            assertEquals(1, countRows(connection, countUsersSql));
            assertEquals(16, countRows(connection, countCategoriesSql));
            assertEquals(2, countRows(connection, countMigrationsSql));
        }
    }

    @Test
    void successfullyEnforcesForeignKeyConstraintsAfterMigration()
            throws IOException, SQLException {

        DataSource dataSource = createTestDataSource();
        DatabaseMigrationManager manager = new DatabaseMigrationManager(dataSource);

        manager.migrate();

        String sql = """
                INSERT INTO ExpenseProfile (UserId, ProfileName)
                VALUES (?, ?)
                """;

        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, 999L);
            statement.setString(2, "Test Profile");

            assertThrows(SQLException.class,
                    statement::executeUpdate);
        }
    }

    /**
     * Creates a SQLite data source pointing to a temporary database file.
     *
     * @return a configured test data source
     * @throws IOException if the database directory cannot be created
     */
    private DataSource createTestDataSource() throws IOException {
        Path databasePath = tempDir.resolve("test-expenses.db");
        return SQLiteDataSourceFactory.create(databasePath);
    }

    /**
     * Executes a count query and returns its result.
     *
     * @param connection the database connection
     * @param sql        a SQL query returning a single count
     * @return the count returned by the query
     * @throws SQLException if query execution fails
     */
    private static int countRows(Connection connection, String sql)
            throws SQLException {

        try (Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(sql)) {

            assertTrue(resultSet.next());
            return resultSet.getInt(1);
        }
    }
}
