package com.expensetracker.persistence.repositories;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import javax.sql.DataSource;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.expensetracker.models.Category;
import com.expensetracker.models.TransactionType;
import com.expensetracker.persistence.DatabaseConnectionManager;
import com.expensetracker.persistence.DatabaseMigrationManager;
import com.expensetracker.persistence.SQLiteDataSourceFactory;
import com.expensetracker.persistence.exceptions.PersistenceException;

/**
 * Tests for {@link SQLiteCategoryRepository}.
 */
class SQLiteCategoryRepositoryTest {

    private static final long CATEGORY_ID = 1L;
    private static final String UPDATED_CATEGORY_NAME = "Updated Category";
    private static final String EXISTING_CATEGORY_NAME = "Existing Category";
    private static final long NONEXISTENT_CATEGORY_ID = 9999L;

    private static final String CATEGORY_NAME = "Custom Category";
    private static final String DUPLICATE_CATEGORY_NAME = "custom category";

    private static final TransactionType CATEGORY_TYPE = TransactionType.EXPENSE;

    private static final String CATEGORY_ID_COLUMN = "CategoryId";
    private static final String NAME_COLUMN = "Name";
    private static final String TYPE_COLUMN = "Type";
    private static final String IS_ACTIVE_COLUMN = "IsActive";

    private static final String SELECT_CATEGORY_BY_ID_SQL = String.format("""
            SELECT %s, %s, %s, %s
            FROM Category
            WHERE %s = ?
            """,
            CATEGORY_ID_COLUMN,
            NAME_COLUMN,
            TYPE_COLUMN,
            IS_ACTIVE_COLUMN,
            CATEGORY_ID_COLUMN);

    @TempDir
    Path tempDir;

    @Nested
    @DisplayName("Tests for SQLiteCategoryRepository constructor validation")
    class ConstructorTests {

        @Test
        void throwsNullPointerExceptionWhenConnectionManagerIsNull() {
            assertThrows(
                    NullPointerException.class,
                    () -> new SQLiteCategoryRepository(null));
        }
    }

    @Nested
    @DisplayName("Tests for saving categories, including validation, persistence, uniqueness, and JDBC error handling")
    class SaveTests {

        @Test
        void throwsNullPointerExceptionWhenCategoryIsNull() {
            DatabaseConnectionManager connectionManager = mock(DatabaseConnectionManager.class);

            SQLiteCategoryRepository repository = new SQLiteCategoryRepository(connectionManager);

            assertThrows(
                    NullPointerException.class,
                    () -> repository.save(null));

            verifyNoInteractions(connectionManager);
        }

        @Test
        void throwsIllegalArgumentExceptionWhenCategoryIdIsNotNull() {
            DatabaseConnectionManager connectionManager = mock(DatabaseConnectionManager.class);

            SQLiteCategoryRepository repository = new SQLiteCategoryRepository(connectionManager);

            Category persistedCategory = Category.reconstitute(
                    CATEGORY_ID,
                    CATEGORY_NAME,
                    CATEGORY_TYPE,
                    true);

            assertThrows(
                    IllegalArgumentException.class,
                    () -> repository.save(persistedCategory));

            verifyNoInteractions(connectionManager);
        }

        @Test
        void successfullyInsertsNewCategoryAndReturnsReconstitutedCategory()
                throws IOException, SQLException {

            DataSource dataSource = SQLiteDataSourceFactory.create(
                    tempDir.resolve("save-category.db"));

            new DatabaseMigrationManager(dataSource).migrate();

            DatabaseConnectionManager connectionManager = new DatabaseConnectionManager(dataSource);

            SQLiteCategoryRepository repository = new SQLiteCategoryRepository(connectionManager);

            Category category = createNewCategory();

            Category savedCategory = repository.save(category);

            // Verify the returned category.
            assertNotNull(savedCategory.getId());
            assertTrue(savedCategory.getId() > 0);

            assertEquals(CATEGORY_NAME, savedCategory.getName());
            assertEquals(CATEGORY_TYPE, savedCategory.getType());
            assertTrue(savedCategory.isActive());

            // Verify the original immutable category is unchanged.
            assertNull(category.getId());

            // Verify the category was actually persisted.
            try (Connection connection = dataSource.getConnection();
                    PreparedStatement statement = connection.prepareStatement(
                            SELECT_CATEGORY_BY_ID_SQL)) {

                statement.setLong(1, savedCategory.getId());

                try (ResultSet resultSet = statement.executeQuery()) {
                    assertTrue(resultSet.next());

                    assertEquals(
                            savedCategory.getId().longValue(),
                            resultSet.getLong(CATEGORY_ID_COLUMN));

                    assertEquals(
                            CATEGORY_NAME,
                            resultSet.getString(NAME_COLUMN));

                    assertEquals(
                            CATEGORY_TYPE.name(),
                            resultSet.getString(TYPE_COLUMN));

                    assertTrue(
                            resultSet.getBoolean(IS_ACTIVE_COLUMN));

                    assertFalse(resultSet.next());
                }
            }
        }

        @Test
        void throwsPersistenceExceptionWhenCategoryNameAlreadyExistsForSameType()
                throws IOException {

            DataSource dataSource = SQLiteDataSourceFactory.create(
                    tempDir.resolve("duplicate-category.db"));

            new DatabaseMigrationManager(dataSource).migrate();

            DatabaseConnectionManager connectionManager = new DatabaseConnectionManager(dataSource);

            SQLiteCategoryRepository repository = new SQLiteCategoryRepository(connectionManager);

            Category savedCategory = repository.save(createNewCategory());

            Category duplicateCategory = Category.builder()
                    .name(DUPLICATE_CATEGORY_NAME)
                    .type(CATEGORY_TYPE)
                    .build();

            PersistenceException actualException = assertThrows(
                    PersistenceException.class,
                    () -> repository.save(duplicateCategory));

            assertTrue(actualException.getCause() instanceof SQLException);

            // Verify the original category was not modified.
            assertEquals(
                    CATEGORY_NAME,
                    savedCategory.getName());
        }

        @Test
        void successfullyInsertsCategoriesWithSameNameButDifferentTypes()
                throws IOException {

            DataSource dataSource = SQLiteDataSourceFactory.create(
                    tempDir.resolve("category-different-types.db"));

            new DatabaseMigrationManager(dataSource).migrate();

            DatabaseConnectionManager connectionManager = new DatabaseConnectionManager(dataSource);

            SQLiteCategoryRepository repository = new SQLiteCategoryRepository(connectionManager);

            Category expenseCategory = Category.builder()
                    .name(CATEGORY_NAME)
                    .type(TransactionType.EXPENSE)
                    .build();

            Category incomeCategory = Category.builder()
                    .name(CATEGORY_NAME)
                    .type(TransactionType.INCOME)
                    .build();

            Category savedExpenseCategory = repository.save(expenseCategory);

            Category savedIncomeCategory = repository.save(incomeCategory);

            assertNotNull(savedExpenseCategory.getId());
            assertNotNull(savedIncomeCategory.getId());

            assertFalse(
                    savedExpenseCategory.getId()
                            .equals(savedIncomeCategory.getId()));

            assertEquals(
                    TransactionType.EXPENSE,
                    savedExpenseCategory.getType());

            assertEquals(
                    TransactionType.INCOME,
                    savedIncomeCategory.getType());

            assertEquals(
                    savedExpenseCategory.getName(),
                    savedIncomeCategory.getName());
        }

        @Test
        void throwsPersistenceExceptionWhenGetConnectionThrowsSQLException()
                throws SQLException {

            DatabaseConnectionManager connectionManager = mock(DatabaseConnectionManager.class);

            SQLException expectedException = new SQLException("Connection failed");

            when(connectionManager.getConnection())
                    .thenThrow(expectedException);

            SQLiteCategoryRepository repository = new SQLiteCategoryRepository(connectionManager);

            PersistenceException actualException = assertThrows(
                    PersistenceException.class,
                    () -> repository.save(createNewCategory()));

            assertSame(expectedException, actualException.getCause());
            assertEquals(
                    "Category could not be created",
                    actualException.getMessage());
        }

        @Test
        void throwsPersistenceExceptionWhenPrepareStatementThrowsSQLException()
                throws SQLException {

            DatabaseConnectionManager connectionManager = mock(DatabaseConnectionManager.class);

            Connection connection = mock(Connection.class);

            SQLException expectedException = new SQLException("Statement preparation failed");

            when(connectionManager.getConnection())
                    .thenReturn(connection);

            when(connection.prepareStatement(anyString()))
                    .thenThrow(expectedException);

            SQLiteCategoryRepository repository = new SQLiteCategoryRepository(connectionManager);

            PersistenceException actualException = assertThrows(
                    PersistenceException.class,
                    () -> repository.save(createNewCategory()));

            assertSame(expectedException, actualException.getCause());

            verify(connection).close();
        }

        @Test
        void throwsPersistenceExceptionWhenExecuteQueryThrowsSQLException()
                throws SQLException {

            JdbcMocks jdbc = createJdbcMocks();

            SQLException expectedException = new SQLException("Insert execution failed");

            when(jdbc.statement().executeQuery())
                    .thenThrow(expectedException);

            SQLiteCategoryRepository repository = new SQLiteCategoryRepository(
                    jdbc.connectionManager());

            PersistenceException actualException = assertThrows(
                    PersistenceException.class,
                    () -> repository.save(createNewCategory()));

            assertSame(expectedException, actualException.getCause());

            verify(jdbc.statement()).close();
            verify(jdbc.connection()).close();
        }

        @Test
        void throwsPersistenceExceptionWhenResultSetNextThrowsSQLException()
                throws SQLException {

            JdbcMocks jdbc = createJdbcMocks();

            SQLException expectedException = new SQLException("Unable to advance result set");

            when(jdbc.resultSet().next())
                    .thenThrow(expectedException);

            SQLiteCategoryRepository repository = new SQLiteCategoryRepository(
                    jdbc.connectionManager());

            PersistenceException actualException = assertThrows(
                    PersistenceException.class,
                    () -> repository.save(createNewCategory()));

            assertSame(expectedException, actualException.getCause());

            verify(jdbc.resultSet()).close();
            verify(jdbc.statement()).close();
            verify(jdbc.connection()).close();
        }

        @Test
        void throwsPersistenceExceptionWhenNoCategoryIdIsGenerated()
                throws SQLException {

            JdbcMocks jdbc = createJdbcMocks();

            when(jdbc.resultSet().next()).thenReturn(false);

            SQLiteCategoryRepository repository = new SQLiteCategoryRepository(
                    jdbc.connectionManager());

            PersistenceException actualException = assertThrows(
                    PersistenceException.class,
                    () -> repository.save(createNewCategory()));

            assertTrue(actualException.getCause() instanceof SQLException);

            assertEquals(
                    "Insert did not generate a categoryId",
                    actualException.getCause().getMessage());

            verify(jdbc.resultSet()).close();
            verify(jdbc.statement()).close();
            verify(jdbc.connection()).close();
        }

        @Test
        void throwsPersistenceExceptionWhenCategoryIdCannotBeRead()
                throws SQLException {

            JdbcMocks jdbc = createJdbcMocks();

            SQLException expectedException = new SQLException("Unable to read category ID");

            when(jdbc.resultSet().next()).thenReturn(true);

            when(jdbc.resultSet().getLong(CATEGORY_ID_COLUMN))
                    .thenThrow(expectedException);

            SQLiteCategoryRepository repository = new SQLiteCategoryRepository(
                    jdbc.connectionManager());

            PersistenceException actualException = assertThrows(
                    PersistenceException.class,
                    () -> repository.save(createNewCategory()));

            assertSame(expectedException, actualException.getCause());

            verify(jdbc.resultSet()).close();
            verify(jdbc.statement()).close();
            verify(jdbc.connection()).close();
        }

        @Test
        void closesJdbcResourcesWhenSaveSucceeds()
                throws SQLException {

            JdbcMocks jdbc = createJdbcMocks();

            when(jdbc.resultSet().next()).thenReturn(true);

            when(jdbc.resultSet().getLong(CATEGORY_ID_COLUMN))
                    .thenReturn(CATEGORY_ID);

            SQLiteCategoryRepository repository = new SQLiteCategoryRepository(
                    jdbc.connectionManager());

            Category savedCategory = repository.save(createNewCategory());

            assertEquals(
                    CATEGORY_ID,
                    savedCategory.getId().longValue());

            assertEquals(CATEGORY_NAME, savedCategory.getName());
            assertEquals(CATEGORY_TYPE, savedCategory.getType());
            assertTrue(savedCategory.isActive());

            verify(jdbc.resultSet()).close();
            verify(jdbc.statement()).close();
            verify(jdbc.connection()).close();
        }
    }

    @Nested
    @DisplayName("Tests for finding categories by ID, including validation, retrieval, and JDBC error handling")
    class FindByIdTests {

        @ParameterizedTest
        @ValueSource(longs = { 0L, -1L })
        void throwsIllegalArgumentExceptionWhenIdIsNotPositive(long id) {
            DatabaseConnectionManager connectionManager = mock(DatabaseConnectionManager.class);

            SQLiteCategoryRepository repository = new SQLiteCategoryRepository(connectionManager);

            assertThrows(
                    IllegalArgumentException.class,
                    () -> repository.findById(id));

            verifyNoInteractions(connectionManager);
        }

        @Test
        void successfullyReturnsCategoryWhenMatchIsFound()
                throws IOException {

            DataSource dataSource = SQLiteDataSourceFactory.create(
                    tempDir.resolve("find-category.db"));

            new DatabaseMigrationManager(dataSource).migrate();

            DatabaseConnectionManager connectionManager = new DatabaseConnectionManager(dataSource);

            SQLiteCategoryRepository repository = new SQLiteCategoryRepository(connectionManager);

            Category savedCategory = repository.save(createNewCategory());

            Optional<Category> result = repository.findById(savedCategory.getId());

            assertTrue(result.isPresent());
            assertEquals(savedCategory, result.get());

            assertEquals(
                    savedCategory.getId(),
                    result.get().getId());

            assertEquals(CATEGORY_NAME, result.get().getName());
            assertEquals(CATEGORY_TYPE, result.get().getType());
            assertTrue(result.get().isActive());
        }

        @Test
        void successfullyReturnsEmptyWhenNoCategoryMatchesId()
                throws IOException {

            DataSource dataSource = SQLiteDataSourceFactory.create(
                    tempDir.resolve("find-missing-category.db"));

            new DatabaseMigrationManager(dataSource).migrate();

            DatabaseConnectionManager connectionManager = new DatabaseConnectionManager(dataSource);

            SQLiteCategoryRepository repository = new SQLiteCategoryRepository(connectionManager);

            // Use an ID greater than those in the seeded categories.
            long nonexistentCategoryId = 9999L;

            Optional<Category> result = repository.findById(nonexistentCategoryId);

            assertTrue(result.isEmpty());
        }

        @Test
        void successfullyReturnsInactiveCategory()
                throws IOException {

            DataSource dataSource = SQLiteDataSourceFactory.create(
                    tempDir.resolve("find-inactive-category.db"));

            new DatabaseMigrationManager(dataSource).migrate();

            DatabaseConnectionManager connectionManager = new DatabaseConnectionManager(dataSource);

            SQLiteCategoryRepository repository = new SQLiteCategoryRepository(connectionManager);

            Category inactiveCategory = createNewCategory().deactivate();

            Category savedCategory = repository.save(inactiveCategory);

            Optional<Category> result = repository.findById(savedCategory.getId());

            assertTrue(result.isPresent());
            assertEquals(savedCategory, result.get());
            assertFalse(result.get().isActive());
        }

        @Test
        void throwsPersistenceExceptionWhenGetConnectionThrowsSQLException()
                throws SQLException {

            DatabaseConnectionManager connectionManager = mock(DatabaseConnectionManager.class);

            SQLException expectedException = new SQLException("Connection failed");

            when(connectionManager.getConnection())
                    .thenThrow(expectedException);

            SQLiteCategoryRepository repository = new SQLiteCategoryRepository(connectionManager);

            PersistenceException actualException = assertThrows(
                    PersistenceException.class,
                    () -> repository.findById(CATEGORY_ID));

            assertSame(expectedException, actualException.getCause());

            assertEquals(
                    "Unable to find category by categoryId",
                    actualException.getMessage());
        }

        @Test
        void throwsPersistenceExceptionWhenPrepareStatementThrowsSQLException()
                throws SQLException {

            DatabaseConnectionManager connectionManager = mock(DatabaseConnectionManager.class);

            Connection connection = mock(Connection.class);

            SQLException expectedException = new SQLException("Statement preparation failed");

            when(connectionManager.getConnection())
                    .thenReturn(connection);

            when(connection.prepareStatement(anyString()))
                    .thenThrow(expectedException);

            SQLiteCategoryRepository repository = new SQLiteCategoryRepository(connectionManager);

            PersistenceException actualException = assertThrows(
                    PersistenceException.class,
                    () -> repository.findById(CATEGORY_ID));

            assertSame(expectedException, actualException.getCause());

            verify(connection).close();
        }

        @Test
        void throwsPersistenceExceptionWhenExecuteQueryThrowsSQLException()
                throws SQLException {

            JdbcMocks jdbc = createJdbcMocks();

            SQLException expectedException = new SQLException("Query execution failed");

            when(jdbc.statement().executeQuery())
                    .thenThrow(expectedException);

            SQLiteCategoryRepository repository = new SQLiteCategoryRepository(
                    jdbc.connectionManager());

            PersistenceException actualException = assertThrows(
                    PersistenceException.class,
                    () -> repository.findById(CATEGORY_ID));

            assertSame(expectedException, actualException.getCause());

            verify(jdbc.statement()).close();
            verify(jdbc.connection()).close();
        }

        @Test
        void throwsPersistenceExceptionWhenResultSetNextThrowsSQLException()
                throws SQLException {

            JdbcMocks jdbc = createJdbcMocks();

            SQLException expectedException = new SQLException("Unable to advance result set");

            when(jdbc.resultSet().next())
                    .thenThrow(expectedException);

            SQLiteCategoryRepository repository = new SQLiteCategoryRepository(
                    jdbc.connectionManager());

            PersistenceException actualException = assertThrows(
                    PersistenceException.class,
                    () -> repository.findById(CATEGORY_ID));

            assertSame(expectedException, actualException.getCause());

            verify(jdbc.resultSet()).close();
            verify(jdbc.statement()).close();
            verify(jdbc.connection()).close();
        }

        @Test
        void throwsPersistenceExceptionWhenCategoryColumnsCannotBeRead()
                throws SQLException {

            JdbcMocks jdbc = createJdbcMocks();

            SQLException expectedException = new SQLException("Unable to read category column");

            when(jdbc.resultSet().next()).thenReturn(true);

            when(jdbc.resultSet().getLong(CATEGORY_ID_COLUMN))
                    .thenReturn(CATEGORY_ID);

            when(jdbc.resultSet().getString(NAME_COLUMN))
                    .thenThrow(expectedException);

            SQLiteCategoryRepository repository = new SQLiteCategoryRepository(
                    jdbc.connectionManager());

            PersistenceException actualException = assertThrows(
                    PersistenceException.class,
                    () -> repository.findById(CATEGORY_ID));

            assertSame(expectedException, actualException.getCause());

            verify(jdbc.resultSet()).close();
            verify(jdbc.statement()).close();
            verify(jdbc.connection()).close();
        }
    }

    @Nested
    @DisplayName("Tests for finding categories by type, including filtering, ordering, and JDBC error handling")
    class FindAllByTypeTests {

        @Test
        void throwsNullPointerExceptionWhenTypeIsNull() {
            DatabaseConnectionManager connectionManager = mock(DatabaseConnectionManager.class);

            SQLiteCategoryRepository repository = new SQLiteCategoryRepository(connectionManager);

            assertThrows(
                    NullPointerException.class,
                    () -> repository.findAllByType(null));

            verifyNoInteractions(connectionManager);
        }

        @Test
        void successfullyReturnsEmptyListWhenNoCategoriesMatchType()
                throws IOException {

            DataSource dataSource = SQLiteDataSourceFactory.create(
                    tempDir.resolve("find-all-empty-income.db"));

            new DatabaseMigrationManager(dataSource).migrate();

            DatabaseConnectionManager connectionManager = new DatabaseConnectionManager(dataSource);

            SQLiteCategoryRepository repository = new SQLiteCategoryRepository(connectionManager);

            // Our migrations seed expense categories, but no income categories.
            List<Category> categories = repository.findAllByType(TransactionType.INCOME);

            assertNotNull(categories);
            assertTrue(categories.isEmpty());
        }

        @Test
        void successfullyReturnsCategoriesOrderedById()
                throws IOException {

            DataSource dataSource = SQLiteDataSourceFactory.create(
                    tempDir.resolve("find-all-expense-categories.db"));

            new DatabaseMigrationManager(dataSource).migrate();

            DatabaseConnectionManager connectionManager = new DatabaseConnectionManager(dataSource);

            SQLiteCategoryRepository repository = new SQLiteCategoryRepository(connectionManager);

            // The migrations seed 16 expense categories.
            List<Category> categories = repository.findAllByType(TransactionType.EXPENSE);

            assertEquals(16, categories.size());

            for (int i = 0; i < categories.size(); i++) {
                Category category = categories.get(i);

                assertNotNull(category.getId());
                assertEquals(TransactionType.EXPENSE, category.getType());

                if (i > 0) {
                    assertTrue(
                            categories.get(i - 1).getId() < category.getId());
                }
            }
        }

        @Test
        void successfullyReturnsOnlyCategoriesMatchingRequestedType()
                throws IOException {

            DataSource dataSource = SQLiteDataSourceFactory.create(
                    tempDir.resolve("find-all-filtered-categories.db"));

            new DatabaseMigrationManager(dataSource).migrate();

            DatabaseConnectionManager connectionManager = new DatabaseConnectionManager(dataSource);

            SQLiteCategoryRepository repository = new SQLiteCategoryRepository(connectionManager);

            Category incomeCategory = Category.builder()
                    .name(CATEGORY_NAME)
                    .type(TransactionType.INCOME)
                    .build();

            Category savedIncomeCategory = repository.save(incomeCategory);

            Category savedExpenseCategory = repository.save(createNewCategory());

            List<Category> incomeCategories = repository.findAllByType(TransactionType.INCOME);

            assertEquals(1, incomeCategories.size());
            assertEquals(savedIncomeCategory, incomeCategories.get(0));

            List<Category> expenseCategories = repository.findAllByType(TransactionType.EXPENSE);

            // 16 seeded categories plus our custom expense category.
            assertEquals(17, expenseCategories.size());

            assertTrue(expenseCategories.contains(savedExpenseCategory));
            assertFalse(expenseCategories.contains(savedIncomeCategory));

            assertTrue(expenseCategories.stream()
                    .allMatch(category -> category.getType() == TransactionType.EXPENSE));
        }

        @Test
        void successfullyIncludesInactiveCategories()
                throws IOException {

            DataSource dataSource = SQLiteDataSourceFactory.create(
                    tempDir.resolve("find-all-inactive-categories.db"));

            new DatabaseMigrationManager(dataSource).migrate();

            DatabaseConnectionManager connectionManager = new DatabaseConnectionManager(dataSource);

            SQLiteCategoryRepository repository = new SQLiteCategoryRepository(connectionManager);

            Category inactiveCategory = createNewCategory().deactivate();

            Category savedCategory = repository.save(inactiveCategory);

            List<Category> categories = repository.findAllByType(CATEGORY_TYPE);

            assertEquals(17, categories.size());
            assertTrue(categories.contains(savedCategory));

            Category retrievedCategory = categories.stream()
                    .filter(category -> category.getId().equals(savedCategory.getId()))
                    .findFirst()
                    .orElseThrow();

            assertFalse(retrievedCategory.isActive());
        }

        @Test
        void throwsPersistenceExceptionWhenGetConnectionThrowsSQLException()
                throws SQLException {

            DatabaseConnectionManager connectionManager = mock(DatabaseConnectionManager.class);

            SQLException expectedException = new SQLException("Connection failed");

            when(connectionManager.getConnection())
                    .thenThrow(expectedException);

            SQLiteCategoryRepository repository = new SQLiteCategoryRepository(connectionManager);

            PersistenceException actualException = assertThrows(
                    PersistenceException.class,
                    () -> repository.findAllByType(CATEGORY_TYPE));

            assertSame(expectedException, actualException.getCause());

            assertEquals(
                    "Unable to find categories by type",
                    actualException.getMessage());
        }

        @Test
        void throwsPersistenceExceptionWhenPrepareStatementThrowsSQLException()
                throws SQLException {

            DatabaseConnectionManager connectionManager = mock(DatabaseConnectionManager.class);

            Connection connection = mock(Connection.class);

            SQLException expectedException = new SQLException("Statement preparation failed");

            when(connectionManager.getConnection())
                    .thenReturn(connection);

            when(connection.prepareStatement(anyString()))
                    .thenThrow(expectedException);

            SQLiteCategoryRepository repository = new SQLiteCategoryRepository(connectionManager);

            PersistenceException actualException = assertThrows(
                    PersistenceException.class,
                    () -> repository.findAllByType(CATEGORY_TYPE));

            assertSame(expectedException, actualException.getCause());

            verify(connection).close();
        }

        @Test
        void throwsPersistenceExceptionWhenExecuteQueryThrowsSQLException()
                throws SQLException {

            JdbcMocks jdbc = createJdbcMocks();

            SQLException expectedException = new SQLException("Query execution failed");

            when(jdbc.statement().executeQuery())
                    .thenThrow(expectedException);

            SQLiteCategoryRepository repository = new SQLiteCategoryRepository(
                    jdbc.connectionManager());

            PersistenceException actualException = assertThrows(
                    PersistenceException.class,
                    () -> repository.findAllByType(CATEGORY_TYPE));

            assertSame(expectedException, actualException.getCause());

            verify(jdbc.statement()).close();
            verify(jdbc.connection()).close();
        }

        @Test
        void throwsPersistenceExceptionWhenResultSetNextThrowsSQLException()
                throws SQLException {

            JdbcMocks jdbc = createJdbcMocks();

            SQLException expectedException = new SQLException("Unable to advance result set");

            when(jdbc.resultSet().next())
                    .thenThrow(expectedException);

            SQLiteCategoryRepository repository = new SQLiteCategoryRepository(
                    jdbc.connectionManager());

            PersistenceException actualException = assertThrows(
                    PersistenceException.class,
                    () -> repository.findAllByType(CATEGORY_TYPE));

            assertSame(expectedException, actualException.getCause());

            verify(jdbc.resultSet()).close();
            verify(jdbc.statement()).close();
            verify(jdbc.connection()).close();
        }

        @Test
        void throwsPersistenceExceptionWhenCategoryColumnsCannotBeRead()
                throws SQLException {

            JdbcMocks jdbc = createJdbcMocks();

            SQLException expectedException = new SQLException("Unable to read category column");

            when(jdbc.resultSet().next()).thenReturn(true);

            when(jdbc.resultSet().getLong(CATEGORY_ID_COLUMN))
                    .thenReturn(CATEGORY_ID);

            when(jdbc.resultSet().getString(NAME_COLUMN))
                    .thenThrow(expectedException);

            SQLiteCategoryRepository repository = new SQLiteCategoryRepository(
                    jdbc.connectionManager());

            PersistenceException actualException = assertThrows(
                    PersistenceException.class,
                    () -> repository.findAllByType(CATEGORY_TYPE));

            assertSame(expectedException, actualException.getCause());

            verify(jdbc.resultSet()).close();
            verify(jdbc.statement()).close();
            verify(jdbc.connection()).close();
        }
    }

    @Nested
    @DisplayName("Tests for updating categories, including validation, renaming, activation, and JDBC error handling")
    class UpdateTests {

        @Test
        void throwsNullPointerExceptionWhenCategoryIsNull() {
            DatabaseConnectionManager connectionManager = mock(DatabaseConnectionManager.class);

            SQLiteCategoryRepository repository = new SQLiteCategoryRepository(connectionManager);

            assertThrows(
                    NullPointerException.class,
                    () -> repository.update(null));

            verifyNoInteractions(connectionManager);
        }

        @Test
        void throwsIllegalArgumentExceptionWhenCategoryIdIsNull() {
            DatabaseConnectionManager connectionManager = mock(DatabaseConnectionManager.class);

            SQLiteCategoryRepository repository = new SQLiteCategoryRepository(connectionManager);

            assertThrows(
                    IllegalArgumentException.class,
                    () -> repository.update(createNewCategory()));

            verifyNoInteractions(connectionManager);
        }

        @Test
        void successfullyRenamesCategory() throws IOException {
            DataSource dataSource = SQLiteDataSourceFactory.create(
                    tempDir.resolve("update-category-name.db"));

            new DatabaseMigrationManager(dataSource).migrate();

            DatabaseConnectionManager connectionManager = new DatabaseConnectionManager(dataSource);

            SQLiteCategoryRepository repository = new SQLiteCategoryRepository(connectionManager);

            Category originalCategory = repository.save(createNewCategory());

            Category categoryToUpdate = originalCategory.rename(
                    UPDATED_CATEGORY_NAME);

            Category updatedCategory = repository.update(categoryToUpdate);

            assertEquals(originalCategory.getId(), updatedCategory.getId());
            assertEquals(UPDATED_CATEGORY_NAME, updatedCategory.getName());
            assertEquals(CATEGORY_TYPE, updatedCategory.getType());
            assertTrue(updatedCategory.isActive());

            // Verify that the updated values are persisted.
            Optional<Category> retrievedCategory = repository.findById(originalCategory.getId());

            assertTrue(retrievedCategory.isPresent());
            assertEquals(updatedCategory, retrievedCategory.get());

            // Verify that the original immutable category is unchanged.
            assertEquals(CATEGORY_NAME, originalCategory.getName());
        }

        @Test
        void successfullyDeactivatesCategory() throws IOException {
            DataSource dataSource = SQLiteDataSourceFactory.create(
                    tempDir.resolve("deactivate-category.db"));

            new DatabaseMigrationManager(dataSource).migrate();

            DatabaseConnectionManager connectionManager = new DatabaseConnectionManager(dataSource);

            SQLiteCategoryRepository repository = new SQLiteCategoryRepository(connectionManager);

            Category originalCategory = repository.save(createNewCategory());

            Category updatedCategory = repository.update(
                    originalCategory.deactivate());

            assertEquals(originalCategory.getId(), updatedCategory.getId());
            assertFalse(updatedCategory.isActive());

            Optional<Category> retrievedCategory = repository.findById(originalCategory.getId());

            assertTrue(retrievedCategory.isPresent());
            assertFalse(retrievedCategory.get().isActive());

            // The original immutable category remains active.
            assertTrue(originalCategory.isActive());
        }

        @Test
        void successfullyActivatesCategory() throws IOException {
            DataSource dataSource = SQLiteDataSourceFactory.create(
                    tempDir.resolve("activate-category.db"));

            new DatabaseMigrationManager(dataSource).migrate();

            DatabaseConnectionManager connectionManager = new DatabaseConnectionManager(dataSource);

            SQLiteCategoryRepository repository = new SQLiteCategoryRepository(connectionManager);

            Category inactiveCategory = repository.save(
                    createNewCategory().deactivate());

            Category updatedCategory = repository.update(
                    inactiveCategory.activate());

            assertEquals(inactiveCategory.getId(), updatedCategory.getId());
            assertTrue(updatedCategory.isActive());

            Optional<Category> retrievedCategory = repository.findById(inactiveCategory.getId());

            assertTrue(retrievedCategory.isPresent());
            assertTrue(retrievedCategory.get().isActive());

            assertFalse(inactiveCategory.isActive());
        }

        @Test
        void throwsIllegalArgumentExceptionWhenNoCategoryUpdated()
                throws IOException {

            DataSource dataSource = SQLiteDataSourceFactory.create(
                    tempDir.resolve("update-missing-category.db"));

            new DatabaseMigrationManager(dataSource).migrate();

            DatabaseConnectionManager connectionManager = new DatabaseConnectionManager(dataSource);

            SQLiteCategoryRepository repository = new SQLiteCategoryRepository(connectionManager);

            Category nonexistentCategory = Category.reconstitute(
                    NONEXISTENT_CATEGORY_ID,
                    UPDATED_CATEGORY_NAME,
                    CATEGORY_TYPE,
                    true);

            IllegalArgumentException actualException = assertThrows(
                    IllegalArgumentException.class,
                    () -> repository.update(nonexistentCategory));

            assertEquals(
                    "category does not exist with id: "
                            + NONEXISTENT_CATEGORY_ID,
                    actualException.getMessage());

            assertTrue(
                    repository.findById(NONEXISTENT_CATEGORY_ID).isEmpty());
        }

        @Test
        void throwsPersistenceExceptionWhenUpdatedNameAlreadyExistsForSameType()
                throws IOException {

            DataSource dataSource = SQLiteDataSourceFactory.create(
                    tempDir.resolve("update-duplicate-category.db"));

            new DatabaseMigrationManager(dataSource).migrate();

            DatabaseConnectionManager connectionManager = new DatabaseConnectionManager(dataSource);

            SQLiteCategoryRepository repository = new SQLiteCategoryRepository(connectionManager);

            Category existingCategory = repository.save(
                    Category.builder()
                            .name(EXISTING_CATEGORY_NAME)
                            .type(CATEGORY_TYPE)
                            .build());

            Category categoryToRename = repository.save(createNewCategory());

            // Deliberately use different capitalization to verify
            // the case-insensitive uniqueness constraint.
            Category duplicateNameCategory = categoryToRename.rename(
                    EXISTING_CATEGORY_NAME.toLowerCase());

            PersistenceException actualException = assertThrows(
                    PersistenceException.class,
                    () -> repository.update(duplicateNameCategory));

            assertTrue(actualException.getCause() instanceof SQLException);

            // Verify neither category was modified.
            assertEquals(
                    existingCategory,
                    repository.findById(existingCategory.getId()).orElseThrow());

            assertEquals(
                    categoryToRename,
                    repository.findById(categoryToRename.getId()).orElseThrow());
        }

        @Test
        void throwsPersistenceExceptionWhenGetConnectionThrowsSQLException()
                throws SQLException {

            DatabaseConnectionManager connectionManager = mock(DatabaseConnectionManager.class);

            SQLException expectedException = new SQLException("Connection failed");

            when(connectionManager.getConnection())
                    .thenThrow(expectedException);

            SQLiteCategoryRepository repository = new SQLiteCategoryRepository(connectionManager);

            Category category = Category.reconstitute(
                    CATEGORY_ID,
                    UPDATED_CATEGORY_NAME,
                    CATEGORY_TYPE,
                    true);

            PersistenceException actualException = assertThrows(
                    PersistenceException.class,
                    () -> repository.update(category));

            assertSame(expectedException, actualException.getCause());
            assertEquals(
                    "Unable to update category",
                    actualException.getMessage());
        }

        @Test
        void throwsPersistenceExceptionWhenPrepareStatementThrowsSQLException()
                throws SQLException {

            DatabaseConnectionManager connectionManager = mock(DatabaseConnectionManager.class);

            Connection connection = mock(Connection.class);

            SQLException expectedException = new SQLException("Statement preparation failed");

            when(connectionManager.getConnection())
                    .thenReturn(connection);

            when(connection.prepareStatement(anyString()))
                    .thenThrow(expectedException);

            SQLiteCategoryRepository repository = new SQLiteCategoryRepository(connectionManager);

            Category category = Category.reconstitute(
                    CATEGORY_ID,
                    UPDATED_CATEGORY_NAME,
                    CATEGORY_TYPE,
                    true);

            PersistenceException actualException = assertThrows(
                    PersistenceException.class,
                    () -> repository.update(category));

            assertSame(expectedException, actualException.getCause());

            verify(connection).close();
        }

        @Test
        void throwsPersistenceExceptionWhenExecuteQueryThrowsSQLException()
                throws SQLException {

            JdbcMocks jdbc = createJdbcMocks();

            SQLException expectedException = new SQLException("Update execution failed");

            when(jdbc.statement().executeQuery())
                    .thenThrow(expectedException);

            SQLiteCategoryRepository repository = new SQLiteCategoryRepository(
                    jdbc.connectionManager());

            Category category = Category.reconstitute(
                    CATEGORY_ID,
                    UPDATED_CATEGORY_NAME,
                    CATEGORY_TYPE,
                    true);

            PersistenceException actualException = assertThrows(
                    PersistenceException.class,
                    () -> repository.update(category));

            assertSame(expectedException, actualException.getCause());

            verify(jdbc.statement()).close();
            verify(jdbc.connection()).close();
        }

        @Test
        void throwsPersistenceExceptionWhenResultSetNextThrowsSQLException()
                throws SQLException {

            JdbcMocks jdbc = createJdbcMocks();

            SQLException expectedException = new SQLException("Unable to advance result set");

            when(jdbc.resultSet().next())
                    .thenThrow(expectedException);

            SQLiteCategoryRepository repository = new SQLiteCategoryRepository(
                    jdbc.connectionManager());

            Category category = Category.reconstitute(
                    CATEGORY_ID,
                    UPDATED_CATEGORY_NAME,
                    CATEGORY_TYPE,
                    true);

            PersistenceException actualException = assertThrows(
                    PersistenceException.class,
                    () -> repository.update(category));

            assertSame(expectedException, actualException.getCause());

            verify(jdbc.resultSet()).close();
            verify(jdbc.statement()).close();
            verify(jdbc.connection()).close();
        }

        @Test
        void throwsPersistenceExceptionWhenCategoryColumnsCannotBeRead()
                throws SQLException {

            JdbcMocks jdbc = createJdbcMocks();

            SQLException expectedException = new SQLException("Unable to read updated category column");

            when(jdbc.resultSet().next()).thenReturn(true);
            when(jdbc.resultSet().getLong(CATEGORY_ID_COLUMN))
                    .thenReturn(CATEGORY_ID);
            when(jdbc.resultSet().getString(NAME_COLUMN))
                    .thenThrow(expectedException);

            SQLiteCategoryRepository repository = new SQLiteCategoryRepository(
                    jdbc.connectionManager());

            Category category = Category.reconstitute(
                    CATEGORY_ID,
                    UPDATED_CATEGORY_NAME,
                    CATEGORY_TYPE,
                    true);

            PersistenceException actualException = assertThrows(
                    PersistenceException.class,
                    () -> repository.update(category));

            assertSame(expectedException, actualException.getCause());

            verify(jdbc.resultSet()).close();
            verify(jdbc.statement()).close();
            verify(jdbc.connection()).close();
        }
    }

    private static Category createNewCategory() {
        return Category.builder()
                .name(CATEGORY_NAME)
                .type(CATEGORY_TYPE)
                .build();
    }

    private static JdbcMocks createJdbcMocks() throws SQLException {
        DatabaseConnectionManager connectionManager = mock(DatabaseConnectionManager.class);

        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(connectionManager.getConnection())
                .thenReturn(connection);

        when(connection.prepareStatement(anyString()))
                .thenReturn(statement);

        when(statement.executeQuery())
                .thenReturn(resultSet);

        return new JdbcMocks(
                connectionManager,
                connection,
                statement,
                resultSet);
    }

    private record JdbcMocks(
            DatabaseConnectionManager connectionManager,
            Connection connection,
            PreparedStatement statement,
            ResultSet resultSet) {
    }
}
