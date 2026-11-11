package com.expensetracker.ui.bootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.expensetracker.models.Category;
import com.expensetracker.models.FinancialTransaction;
import com.expensetracker.models.Necessity;
import com.expensetracker.models.TransactionType;
import com.expensetracker.models.User;

/**
 * Integration tests for {@link ApplicationBootstrap}.
 *
 * <p>
 * Uses temporary SQLite databases to verify database initialization,
 * Flyway migrations, repository wiring, service construction,
 * persistence, and foreign-key enforcement.
 *
 * <p>
 * Each test uses an isolated temporary directory provided by JUnit.
 */
@DisplayName("ApplicationBootstrap Integration Tests")
class ApplicationBootstrapIntegrationTest {

    @TempDir
    Path tempDir;

    private Path databasePath() {
        return tempDir.resolve("expense-tracker.db");
    }

    private User createUser(ApplicationContext context) {
        return context.getUserService().addUser(
                User.builder()
                        .firstName("firstName")
                        .lastName("lastName")
                        .build());
    }

    @Test
    @DisplayName("Initializes a fresh SQLite database")
    void initializesFreshSqliteDatabase() throws IOException {
        Path databasePath = databasePath();

        assertFalse(Files.exists(databasePath));

        ApplicationContext context = ApplicationBootstrap.initialize(databasePath);

        assertNotNull(context);
        assertTrue(Files.isRegularFile(databasePath));

        // A successful insert verifies that the required user
        // table was created by the database migrations.
        User created = createUser(context);

        assertNotNull(created.getId());
        assertTrue(created.getId() > 0);
    }

    @Test
    @DisplayName("Initializes all four application services")
    void initializesAllFourApplicationServices() throws IOException {
        ApplicationContext context = ApplicationBootstrap.initialize(databasePath());

        assertNotNull(context.getUserService());
        assertNotNull(context.getExpenseProfileService());
        assertNotNull(context.getCategoryService());
        assertNotNull(context.getFinancialTransactionService());
    }

    @Test
    @DisplayName("Preserves data across application initializations")
    void preservesDataAcrossApplicationInitializations()
            throws IOException {

        Path databasePath = databasePath();

        ApplicationContext firstContext = ApplicationBootstrap.initialize(databasePath);

        User created = createUser(firstContext);

        long userId = created.getId();

        // Initialize the application again against the
        // same database file.
        ApplicationContext secondContext = ApplicationBootstrap.initialize(databasePath);

        Optional<User> retrieved = secondContext.getUserService().getUserById(userId);

        assertTrue(retrieved.isPresent());
        assertEquals(userId, retrieved.get().getId());
        assertEquals("firstName", retrieved.get().getFirstName());
        assertEquals("lastName", retrieved.get().getLastName());
    }

    @Test
    @DisplayName("Creates and retrieves a category through initialized services")
    void createsAndRetrievesCategoryThroughInitializedServices()
            throws IOException {

        ApplicationContext context = ApplicationBootstrap.initialize(databasePath());

        Category category = Category.builder()
                .name("Integration Test Category")
                .type(TransactionType.EXPENSE)
                .build();

        Category created = context.getCategoryService().createCategory(category);

        assertNotNull(created.getId());
        assertTrue(created.isActive());

        Optional<Category> retrieved = context.getCategoryService().getCategoryById(
                created.getId());

        assertTrue(retrieved.isPresent());
        assertEquals("Integration Test Category", retrieved.get().getName());
        assertEquals(
                TransactionType.EXPENSE,
                retrieved.get().getType());
    }

    @Test
    @DisplayName("Enforces foreign key constraints for transaction profiles")
    void enforcesForeignKeyConstraintsForTransactionProfiles()
            throws IOException {

        ApplicationContext context = ApplicationBootstrap.initialize(databasePath());

        Category category = context.getCategoryService().createCategory(
                Category.builder()
                        .name("Integration Test Category")
                        .type(TransactionType.EXPENSE)
                        .build());

        // This profile ID is positive, but no profile
        // with this ID has been created.
        long nonexistentProfileId = 999999L;

        FinancialTransaction transaction = FinancialTransaction.builder()
                .profileId(nonexistentProfileId)
                .categoryId(category.getId())
                .amount(2500L)
                .type(TransactionType.EXPENSE)
                .transactionDate(LocalDate.of(2026, 11, 10))
                .necessity(Necessity.NECESSARY)
                .description("Integration Test Category")
                .build();

        // Category validation succeeds. SQLite must reject
        // the nonexistent expense profile.
        assertThrows(
                RuntimeException.class,
                () -> context.getFinancialTransactionService()
                        .createFinancialTransaction(transaction));
    }

    @Test
    @DisplayName("Fails initialization when database parent is a regular file")
    void failsInitializationWhenDatabaseParentIsRegularFile()
            throws IOException {

        Path invalidParent = tempDir.resolve("not-a-directory");

        Files.writeString(invalidParent, "This is a regular file.");

        Path invalidDatabasePath = invalidParent.resolve("expense-tracker.db");

        assertThrows(
                Exception.class,
                () -> ApplicationBootstrap.initialize(invalidDatabasePath));
    }
}
