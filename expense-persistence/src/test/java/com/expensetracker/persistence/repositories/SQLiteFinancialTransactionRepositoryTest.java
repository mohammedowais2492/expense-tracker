package com.expensetracker.persistence.repositories;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
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
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.expensetracker.models.Category;
import com.expensetracker.models.ExpenseProfile;
import com.expensetracker.models.FinancialTransaction;
import com.expensetracker.models.FinancialTransactionQuery;
import com.expensetracker.models.Necessity;
import com.expensetracker.models.Page;
import com.expensetracker.models.PageRequest;
import com.expensetracker.models.TransactionType;
import com.expensetracker.models.User;
import com.expensetracker.persistence.DatabaseConnectionManager;
import com.expensetracker.persistence.DatabaseMigrationManager;
import com.expensetracker.persistence.SQLiteDataSourceFactory;
import com.expensetracker.persistence.exceptions.PersistenceException;

/**
 * Tests for {@link SQLiteFinancialTransactionRepository}.
 */
class SQLiteFinancialTransactionRepositoryTest {

        private static final long TRANSACTION_ID = 1L;
        private static final long AMOUNT = 1250L;
        private static final String DESCRIPTION = "Grocery shopping";
        private static final LocalDate TRANSACTION_DATE = LocalDate.of(2026, 11, 8);

        private static final String TRANSACTION_ID_COLUMN = "TransactionId";
        private static final String PROFILE_ID_COLUMN = "ProfileId";
        private static final String CATEGORY_ID_COLUMN = "CategoryId";
        private static final String TYPE_COLUMN = "Type";
        private static final String AMOUNT_COLUMN = "Amount";
        private static final String DESCRIPTION_COLUMN = "Description";
        private static final String TRANSACTION_DATE_COLUMN = "TransactionDate";
        private static final String NECESSITY_COLUMN = "Necessity";

        private static final String SELECT_TRANSACTION_SQL = String.format("""
                        SELECT %s, %s, %s, %s, %s, %s, %s, %s
                        FROM FinancialTransaction
                        WHERE %s = ?
                        """,
                        TRANSACTION_ID_COLUMN,
                        PROFILE_ID_COLUMN,
                        CATEGORY_ID_COLUMN,
                        TYPE_COLUMN,
                        AMOUNT_COLUMN,
                        DESCRIPTION_COLUMN,
                        TRANSACTION_DATE_COLUMN,
                        NECESSITY_COLUMN,
                        TRANSACTION_ID_COLUMN);

        @TempDir
        Path tempDir;

        private TestFixture createFixture(String databaseName)
                        throws IOException {

                DataSource dataSource = SQLiteDataSourceFactory.create(
                                tempDir.resolve(databaseName));

                new DatabaseMigrationManager(dataSource).migrate();

                DatabaseConnectionManager connectionManager = new DatabaseConnectionManager(dataSource);

                User user = new SQLiteUserRepository(connectionManager).save(
                                User.builder()
                                                .firstName("firstName")
                                                .build());

                ExpenseProfile profile = new SQLiteExpenseProfileRepository(connectionManager).save(
                                ExpenseProfile.builder()
                                                .userId(user.getId())
                                                .profileName("Test Profile")
                                                .build());

                SQLiteCategoryRepository categoryRepository = new SQLiteCategoryRepository(connectionManager);

                Category expenseCategory = categoryRepository.save(
                                Category.builder()
                                                .name("Custom Expense")
                                                .type(TransactionType.EXPENSE)
                                                .build());

                Category incomeCategory = categoryRepository.save(
                                Category.builder()
                                                .name("Custom Income")
                                                .type(TransactionType.INCOME)
                                                .build());

                SQLiteFinancialTransactionRepository repository = new SQLiteFinancialTransactionRepository(
                                connectionManager);

                return new TestFixture(
                                dataSource,
                                repository,
                                profile.getId(),
                                expenseCategory.getId(),
                                incomeCategory.getId());
        }

        private static FinancialTransaction createExpenseTransaction(
                        long profileId, long categoryId) {

                return FinancialTransaction.builder()
                                .profileId(profileId)
                                .categoryId(categoryId)
                                .amount(AMOUNT)
                                .type(TransactionType.EXPENSE)
                                .transactionDate(TRANSACTION_DATE)
                                .description(DESCRIPTION)
                                .necessity(Necessity.NECESSARY)
                                .build();
        }

        private static FinancialTransaction createIncomeTransaction(
                        long profileId, long categoryId) {

                return FinancialTransaction.builder()
                                .profileId(profileId)
                                .categoryId(categoryId)
                                .amount(AMOUNT)
                                .type(TransactionType.INCOME)
                                .transactionDate(TRANSACTION_DATE)
                                .build();
        }

        private static JdbcMocks createJdbcMocks()
                        throws SQLException {

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

        private record TestFixture(
                        DataSource dataSource,
                        SQLiteFinancialTransactionRepository repository,
                        long profileId,
                        long expenseCategoryId,
                        long incomeCategoryId) {
        }

        private record JdbcMocks(
                        DatabaseConnectionManager connectionManager,
                        Connection connection,
                        PreparedStatement statement,
                        ResultSet resultSet) {
        }

        @Nested
        @DisplayName("Constructor tests")
        class ConstructorTests {

                @Test
                void throwsNullPointerExceptionWhenConnectionManagerIsNull() {
                        assertThrows(
                                        NullPointerException.class,
                                        () -> new SQLiteFinancialTransactionRepository(null));
                }
        }

        @Nested
        @DisplayName("Save tests")
        class SaveTests {

                @Test
                void throwsNullPointerExceptionWhenTransactionIsNull() {
                        DatabaseConnectionManager connectionManager = mock(DatabaseConnectionManager.class);

                        SQLiteFinancialTransactionRepository repository = new SQLiteFinancialTransactionRepository(
                                        connectionManager);

                        assertThrows(
                                        NullPointerException.class,
                                        () -> repository.save(null));

                        verifyNoInteractions(connectionManager);
                }

                @Test
                void throwsIllegalArgumentExceptionWhenTransactionIdIsNotNull() {
                        DatabaseConnectionManager connectionManager = mock(DatabaseConnectionManager.class);

                        SQLiteFinancialTransactionRepository repository = new SQLiteFinancialTransactionRepository(
                                        connectionManager);

                        FinancialTransaction transaction = FinancialTransaction.reconstitute(
                                        TRANSACTION_ID,
                                        1L,
                                        1L,
                                        AMOUNT,
                                        TransactionType.EXPENSE,
                                        TRANSACTION_DATE,
                                        DESCRIPTION,
                                        Necessity.NECESSARY);

                        assertThrows(
                                        IllegalArgumentException.class,
                                        () -> repository.save(transaction));

                        verifyNoInteractions(connectionManager);
                }

                @Test
                void successfullyInsertsExpenseTransactionAndReturnsReconstitutedTransaction()
                                throws IOException, SQLException {

                        TestFixture fixture = createFixture(
                                        "save-expense-transaction.db");

                        FinancialTransaction transaction = createExpenseTransaction(
                                        fixture.profileId(),
                                        fixture.expenseCategoryId());

                        FinancialTransaction savedTransaction = fixture.repository().save(transaction);

                        assertNotNull(savedTransaction.getId());
                        assertTrue(savedTransaction.getId() > 0);
                        assertEquals(fixture.profileId(), savedTransaction.getProfileId());
                        assertEquals(fixture.expenseCategoryId(), savedTransaction.getCategoryId());
                        assertEquals(TransactionType.EXPENSE, savedTransaction.getType());
                        assertEquals(AMOUNT, savedTransaction.getAmount());
                        assertEquals(DESCRIPTION, savedTransaction.getDescription());
                        assertEquals(TRANSACTION_DATE, savedTransaction.getTransactionDate());
                        assertEquals(Necessity.NECESSARY, savedTransaction.getNecessity());

                        assertNull(transaction.getId());

                        try (Connection connection = fixture.dataSource().getConnection();
                                        PreparedStatement statement = connection
                                                        .prepareStatement(SELECT_TRANSACTION_SQL)) {

                                statement.setLong(1, savedTransaction.getId());

                                try (ResultSet resultSet = statement.executeQuery()) {
                                        assertTrue(resultSet.next());

                                        assertEquals(
                                                        savedTransaction.getId().longValue(),
                                                        resultSet.getLong(TRANSACTION_ID_COLUMN));
                                        assertEquals(
                                                        fixture.profileId(),
                                                        resultSet.getLong(PROFILE_ID_COLUMN));
                                        assertEquals(
                                                        fixture.expenseCategoryId(),
                                                        resultSet.getLong(CATEGORY_ID_COLUMN));
                                        assertEquals(
                                                        TransactionType.EXPENSE.name(),
                                                        resultSet.getString(TYPE_COLUMN));
                                        assertEquals(AMOUNT, resultSet.getLong(AMOUNT_COLUMN));
                                        assertEquals(DESCRIPTION, resultSet.getString(DESCRIPTION_COLUMN));
                                        assertEquals(
                                                        TRANSACTION_DATE.toString(),
                                                        resultSet.getString(TRANSACTION_DATE_COLUMN));
                                        assertEquals(
                                                        Necessity.NECESSARY.name(),
                                                        resultSet.getString(NECESSITY_COLUMN));

                                        assertFalse(resultSet.next());
                                }
                        }
                }

                @Test
                void successfullyInsertsIncomeTransactionWithNullNecessityAndDescription()
                                throws IOException, SQLException {

                        TestFixture fixture = createFixture(
                                        "save-income-transaction.db");

                        FinancialTransaction transaction = createIncomeTransaction(
                                        fixture.profileId(),
                                        fixture.incomeCategoryId());

                        FinancialTransaction savedTransaction = fixture.repository().save(transaction);

                        assertNotNull(savedTransaction.getId());
                        assertEquals(TransactionType.INCOME, savedTransaction.getType());
                        assertNull(savedTransaction.getDescription());
                        assertNull(savedTransaction.getNecessity());

                        try (Connection connection = fixture.dataSource().getConnection();
                                        PreparedStatement statement = connection
                                                        .prepareStatement(SELECT_TRANSACTION_SQL)) {

                                statement.setLong(1, savedTransaction.getId());

                                try (ResultSet resultSet = statement.executeQuery()) {
                                        assertTrue(resultSet.next());

                                        assertEquals(
                                                        TransactionType.INCOME.name(),
                                                        resultSet.getString(TYPE_COLUMN));
                                        assertNull(resultSet.getString(DESCRIPTION_COLUMN));
                                        assertNull(resultSet.getString(NECESSITY_COLUMN));
                                }
                        }
                }

                @Test
                void throwsPersistenceExceptionWhenCategoryTypeDoesNotMatchTransactionType()
                                throws IOException {

                        TestFixture fixture = createFixture(
                                        "save-mismatched-category.db");

                        FinancialTransaction transaction = createExpenseTransaction(
                                        fixture.profileId(),
                                        fixture.incomeCategoryId());

                        PersistenceException actualException = assertThrows(
                                        PersistenceException.class,
                                        () -> fixture.repository().save(transaction));

                        assertTrue(actualException.getCause() instanceof SQLException);
                }

                @Test
                void throwsPersistenceExceptionWhenProfileDoesNotExist()
                                throws IOException {

                        TestFixture fixture = createFixture(
                                        "save-missing-profile.db");

                        FinancialTransaction transaction = createExpenseTransaction(
                                        9999L,
                                        fixture.expenseCategoryId());

                        PersistenceException actualException = assertThrows(
                                        PersistenceException.class,
                                        () -> fixture.repository().save(transaction));

                        assertTrue(actualException.getCause() instanceof SQLException);
                }

                @Test
                void throwsPersistenceExceptionWhenGetConnectionThrowsSQLException()
                                throws SQLException {

                        DatabaseConnectionManager connectionManager = mock(DatabaseConnectionManager.class);

                        SQLException expectedException = new SQLException("Connection failed");

                        when(connectionManager.getConnection())
                                        .thenThrow(expectedException);

                        SQLiteFinancialTransactionRepository repository = new SQLiteFinancialTransactionRepository(
                                        connectionManager);

                        FinancialTransaction transaction = createExpenseTransaction(1L, 1L);

                        PersistenceException actualException = assertThrows(
                                        PersistenceException.class,
                                        () -> repository.save(transaction));

                        assertSame(expectedException, actualException.getCause());
                        assertEquals(
                                        "FinancialTransaction could not be created",
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

                        SQLiteFinancialTransactionRepository repository = new SQLiteFinancialTransactionRepository(
                                        connectionManager);

                        PersistenceException actualException = assertThrows(
                                        PersistenceException.class,
                                        () -> repository.save(createExpenseTransaction(1L, 1L)));

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

                        SQLiteFinancialTransactionRepository repository = new SQLiteFinancialTransactionRepository(
                                        jdbc.connectionManager());

                        PersistenceException actualException = assertThrows(
                                        PersistenceException.class,
                                        () -> repository.save(createExpenseTransaction(1L, 1L)));

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

                        SQLiteFinancialTransactionRepository repository = new SQLiteFinancialTransactionRepository(
                                        jdbc.connectionManager());

                        PersistenceException actualException = assertThrows(
                                        PersistenceException.class,
                                        () -> repository.save(createExpenseTransaction(1L, 1L)));

                        assertSame(expectedException, actualException.getCause());

                        verify(jdbc.resultSet()).close();
                        verify(jdbc.statement()).close();
                        verify(jdbc.connection()).close();
                }

                @Test
                void throwsPersistenceExceptionWhenNoTransactionIdIsGenerated()
                                throws SQLException {

                        JdbcMocks jdbc = createJdbcMocks();

                        when(jdbc.resultSet().next()).thenReturn(false);

                        SQLiteFinancialTransactionRepository repository = new SQLiteFinancialTransactionRepository(
                                        jdbc.connectionManager());

                        PersistenceException actualException = assertThrows(
                                        PersistenceException.class,
                                        () -> repository.save(createExpenseTransaction(1L, 1L)));

                        assertInstanceOf(
                                        SQLException.class,
                                        actualException.getCause());

                        assertEquals(
                                        "Insert did not return a generated transactionId",
                                        actualException.getCause().getMessage());

                        verify(jdbc.resultSet()).close();
                        verify(jdbc.statement()).close();
                        verify(jdbc.connection()).close();
                }

                @Test
                void throwsPersistenceExceptionWhenTransactionIdCannotBeRead()
                                throws SQLException {

                        JdbcMocks jdbc = createJdbcMocks();

                        SQLException expectedException = new SQLException("Unable to read transaction ID");

                        when(jdbc.resultSet().next()).thenReturn(true);

                        when(jdbc.resultSet().getLong(TRANSACTION_ID_COLUMN))
                                        .thenThrow(expectedException);

                        SQLiteFinancialTransactionRepository repository = new SQLiteFinancialTransactionRepository(
                                        jdbc.connectionManager());

                        PersistenceException actualException = assertThrows(
                                        PersistenceException.class,
                                        () -> repository.save(createExpenseTransaction(1L, 1L)));

                        assertSame(expectedException, actualException.getCause());

                        verify(jdbc.resultSet()).close();
                        verify(jdbc.statement()).close();
                        verify(jdbc.connection()).close();
                }

                @Test
                void bindsNullOptionalFieldsAndClosesJdbcResourcesWhenSaveSucceeds()
                                throws SQLException {

                        JdbcMocks jdbc = createJdbcMocks();

                        when(jdbc.resultSet().next()).thenReturn(true);
                        when(jdbc.resultSet().getLong(TRANSACTION_ID_COLUMN))
                                        .thenReturn(TRANSACTION_ID);

                        SQLiteFinancialTransactionRepository repository = new SQLiteFinancialTransactionRepository(
                                        jdbc.connectionManager());

                        FinancialTransaction transaction = createIncomeTransaction(1L, 2L);

                        FinancialTransaction savedTransaction = repository.save(transaction);

                        assertEquals(
                                        TRANSACTION_ID,
                                        savedTransaction.getId().longValue());

                        verify(jdbc.statement()).setLong(1, 1L);
                        verify(jdbc.statement()).setLong(2, 2L);
                        verify(jdbc.statement()).setString(
                                        3, TransactionType.INCOME.name());
                        verify(jdbc.statement()).setLong(4, AMOUNT);
                        verify(jdbc.statement()).setNull(5, Types.VARCHAR);
                        verify(jdbc.statement()).setString(
                                        6, TRANSACTION_DATE.toString());
                        verify(jdbc.statement()).setNull(7, Types.VARCHAR);

                        verify(jdbc.resultSet()).close();
                        verify(jdbc.statement()).close();
                        verify(jdbc.connection()).close();
                }
        }

        @Nested
        @DisplayName("Tests for finding financial transactions by ID")
        class FindByIdTests {

                @ParameterizedTest
                @ValueSource(longs = { 0L, -1L })
                void throwsIllegalArgumentExceptionWhenIdIsNotPositive(long id) {
                        DatabaseConnectionManager connectionManager = mock(DatabaseConnectionManager.class);

                        SQLiteFinancialTransactionRepository repository = new SQLiteFinancialTransactionRepository(
                                        connectionManager);

                        assertThrows(
                                        IllegalArgumentException.class,
                                        () -> repository.findById(id));

                        verifyNoInteractions(connectionManager);
                }

                @Test
                void successfullyReturnsExpenseTransactionWhenMatchIsFound()
                                throws IOException {

                        TestFixture fixture = createFixture(
                                        "find-expense-transaction.db");

                        FinancialTransaction savedTransaction = fixture.repository().save(
                                        createExpenseTransaction(
                                                        fixture.profileId(),
                                                        fixture.expenseCategoryId()));

                        Optional<FinancialTransaction> result = fixture.repository().findById(savedTransaction.getId());

                        assertTrue(result.isPresent());
                        assertEquals(savedTransaction, result.get());
                        assertEquals(TransactionType.EXPENSE, result.get().getType());
                        assertEquals(Necessity.NECESSARY, result.get().getNecessity());
                        assertEquals(DESCRIPTION, result.get().getDescription());
                        assertEquals(TRANSACTION_DATE, result.get().getTransactionDate());
                }

                @Test
                void successfullyReturnsIncomeTransactionWithNullOptionalFields()
                                throws IOException {

                        TestFixture fixture = createFixture(
                                        "find-income-transaction.db");

                        FinancialTransaction savedTransaction = fixture.repository().save(
                                        createIncomeTransaction(
                                                        fixture.profileId(),
                                                        fixture.incomeCategoryId()));

                        Optional<FinancialTransaction> result = fixture.repository().findById(savedTransaction.getId());

                        assertTrue(result.isPresent());
                        assertEquals(savedTransaction, result.get());
                        assertEquals(TransactionType.INCOME, result.get().getType());
                        assertNull(result.get().getDescription());
                        assertNull(result.get().getNecessity());
                }

                @Test
                void successfullyReturnsEmptyWhenNoTransactionMatchesId()
                                throws IOException {

                        TestFixture fixture = createFixture(
                                        "find-missing-transaction.db");

                        Optional<FinancialTransaction> result = fixture.repository().findById(9999L);

                        assertTrue(result.isEmpty());
                }

                @Test
                void throwsPersistenceExceptionWhenGetConnectionThrowsSQLException()
                                throws SQLException {

                        DatabaseConnectionManager connectionManager = mock(DatabaseConnectionManager.class);

                        SQLException expectedException = new SQLException("Connection failed");

                        when(connectionManager.getConnection())
                                        .thenThrow(expectedException);

                        SQLiteFinancialTransactionRepository repository = new SQLiteFinancialTransactionRepository(
                                        connectionManager);

                        PersistenceException actualException = assertThrows(
                                        PersistenceException.class,
                                        () -> repository.findById(TRANSACTION_ID));

                        assertSame(expectedException, actualException.getCause());
                        assertEquals(
                                        "Unable to find financial transaction by id",
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

                        SQLiteFinancialTransactionRepository repository = new SQLiteFinancialTransactionRepository(
                                        connectionManager);

                        PersistenceException actualException = assertThrows(
                                        PersistenceException.class,
                                        () -> repository.findById(TRANSACTION_ID));

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

                        SQLiteFinancialTransactionRepository repository = new SQLiteFinancialTransactionRepository(
                                        jdbc.connectionManager());

                        PersistenceException actualException = assertThrows(
                                        PersistenceException.class,
                                        () -> repository.findById(TRANSACTION_ID));

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

                        SQLiteFinancialTransactionRepository repository = new SQLiteFinancialTransactionRepository(
                                        jdbc.connectionManager());

                        PersistenceException actualException = assertThrows(
                                        PersistenceException.class,
                                        () -> repository.findById(TRANSACTION_ID));

                        assertSame(expectedException, actualException.getCause());
                        verify(jdbc.resultSet()).close();
                        verify(jdbc.statement()).close();
                        verify(jdbc.connection()).close();
                }

                @Test
                void throwsPersistenceExceptionWhenTransactionColumnsCannotBeRead()
                                throws SQLException {

                        JdbcMocks jdbc = createJdbcMocks();

                        SQLException expectedException = new SQLException("Unable to read transaction column");

                        when(jdbc.resultSet().next()).thenReturn(true);
                        when(jdbc.resultSet().getLong(TRANSACTION_ID_COLUMN))
                                        .thenReturn(TRANSACTION_ID);
                        when(jdbc.resultSet().getLong(PROFILE_ID_COLUMN))
                                        .thenThrow(expectedException);

                        SQLiteFinancialTransactionRepository repository = new SQLiteFinancialTransactionRepository(
                                        jdbc.connectionManager());

                        PersistenceException actualException = assertThrows(
                                        PersistenceException.class,
                                        () -> repository.findById(TRANSACTION_ID));

                        assertSame(expectedException, actualException.getCause());
                        verify(jdbc.resultSet()).close();
                        verify(jdbc.statement()).close();
                        verify(jdbc.connection()).close();
                }

                @Test
                void closesJdbcResourcesWhenTransactionIsFound()
                                throws SQLException {

                        JdbcMocks jdbc = createJdbcMocks();

                        when(jdbc.resultSet().next()).thenReturn(true);
                        when(jdbc.resultSet().getLong(TRANSACTION_ID_COLUMN))
                                        .thenReturn(TRANSACTION_ID);
                        when(jdbc.resultSet().getLong(PROFILE_ID_COLUMN))
                                        .thenReturn(1L);
                        when(jdbc.resultSet().getLong(CATEGORY_ID_COLUMN))
                                        .thenReturn(2L);
                        when(jdbc.resultSet().getString(TYPE_COLUMN))
                                        .thenReturn(TransactionType.EXPENSE.name());
                        when(jdbc.resultSet().getLong(AMOUNT_COLUMN))
                                        .thenReturn(AMOUNT);
                        when(jdbc.resultSet().getString(DESCRIPTION_COLUMN))
                                        .thenReturn(DESCRIPTION);
                        when(jdbc.resultSet().getString(TRANSACTION_DATE_COLUMN))
                                        .thenReturn(TRANSACTION_DATE.toString());
                        when(jdbc.resultSet().getString(NECESSITY_COLUMN))
                                        .thenReturn(Necessity.NECESSARY.name());

                        SQLiteFinancialTransactionRepository repository = new SQLiteFinancialTransactionRepository(
                                        jdbc.connectionManager());

                        Optional<FinancialTransaction> result = repository.findById(TRANSACTION_ID);

                        assertTrue(result.isPresent());
                        assertEquals(TRANSACTION_ID, result.get().getId().longValue());

                        verify(jdbc.statement()).setLong(1, TRANSACTION_ID);
                        verify(jdbc.resultSet()).close();
                        verify(jdbc.statement()).close();
                        verify(jdbc.connection()).close();
                }
        }

        @Nested
        @TestInstance(TestInstance.Lifecycle.PER_CLASS)
        @DisplayName("Tests for transaction filtering and keyset pagination")
        class FindAllTests {

                private static final LocalDate NOV_03 = LocalDate.of(2026, 11, 3);
                private static final LocalDate NOV_05 = LocalDate.of(2026, 11, 5);
                private static final LocalDate NOV_06 = LocalDate.of(2026, 11, 6);
                private static final LocalDate NOV_07 = LocalDate.of(2026, 11, 7);
                private static final LocalDate NOV_08 = LocalDate.of(2026, 11, 8);
                private static final LocalDate NOV_09 = LocalDate.of(2026, 11, 9);

                private FinancialTransactionQuery expenseQuery(long profileId) {
                        return FinancialTransactionQuery.builder()
                                        .profileId(profileId)
                                        .type(TransactionType.EXPENSE)
                                        .build();
                }

                private PageRequest pageRequest(int size, String token) {
                        return PageRequest.builder()
                                        .pageSize(size)
                                        .pageToken(token)
                                        .build();
                }

                private FinancialTransaction insertExpense(
                                TestFixture fixture,
                                long categoryId,
                                Necessity necessity,
                                LocalDate date,
                                String description) {

                        FinancialTransaction transaction = FinancialTransaction.builder()
                                        .profileId(fixture.profileId())
                                        .categoryId(categoryId)
                                        .type(TransactionType.EXPENSE)
                                        .amount(AMOUNT)
                                        .description(description)
                                        .transactionDate(date)
                                        .necessity(necessity)
                                        .build();

                        return fixture.repository().save(transaction);
                }

                private Stream<Arguments> filterCombinations() {
                        return Stream.of(
                                        Arguments.of(
                                                        "No optional filters",
                                                        false, false, false, false,
                                                        "F,E,C,B,A,D"),
                                        Arguments.of(
                                                        "Category only",
                                                        true, false, false, false,
                                                        "F,E,B,A,D"),
                                        Arguments.of(
                                                        "Necessity only",
                                                        false, true, false, false,
                                                        "F,E,C,A,D"),
                                        Arguments.of(
                                                        "Start date only",
                                                        false, false, true, false,
                                                        "F,E,C,B,A"),
                                        Arguments.of(
                                                        "End date only",
                                                        false, false, false, true,
                                                        "C,B,A,D"),
                                        Arguments.of(
                                                        "Inclusive date range",
                                                        false, false, true, true,
                                                        "C,B,A"),
                                        Arguments.of(
                                                        "All optional filters",
                                                        true, true, true, true,
                                                        "A"),
                                        Arguments.of(
                                                        "Category and necessity",
                                                        true, true, false, false,
                                                        "F,E,A,D"));
                }

                @Test
                void throwsNullPointerExceptionWhenQueryIsNull() {
                        DatabaseConnectionManager connectionManager = mock(DatabaseConnectionManager.class);

                        SQLiteFinancialTransactionRepository repository = new SQLiteFinancialTransactionRepository(
                                        connectionManager);

                        assertThrows(
                                        NullPointerException.class,
                                        () -> repository.findAll(
                                                        null,
                                                        pageRequest(10, null)));

                        verifyNoInteractions(connectionManager);
                }

                @Test
                void throwsNullPointerExceptionWhenPageRequestIsNull() {
                        DatabaseConnectionManager connectionManager = mock(DatabaseConnectionManager.class);

                        SQLiteFinancialTransactionRepository repository = new SQLiteFinancialTransactionRepository(
                                        connectionManager);

                        assertThrows(
                                        NullPointerException.class,
                                        () -> repository.findAll(
                                                        expenseQuery(1L),
                                                        null));

                        verifyNoInteractions(connectionManager);
                }

                @ParameterizedTest
                @ValueSource(strings = {
                                "",
                                "invalid",
                                "2026-11-08",
                                ":123",
                                "2026-11-08:",
                                "invalid:123",
                                "2026-11-08:abc",
                                "2026-11-08:0",
                                "2026-11-08:-1",
                                "2026-11-08:123:456",
                                "2026-02-30:123",
                                "2026-11-08:9223372036854775808"
                })
                void throwsIllegalArgumentExceptionWhenPageTokenIsInvalid(
                                String token) {

                        DatabaseConnectionManager connectionManager = mock(DatabaseConnectionManager.class);

                        SQLiteFinancialTransactionRepository repository = new SQLiteFinancialTransactionRepository(
                                        connectionManager);

                        assertThrows(
                                        IllegalArgumentException.class,
                                        () -> repository.findAll(
                                                        expenseQuery(1L),
                                                        pageRequest(10, token)));

                        verifyNoInteractions(connectionManager);
                }

                @ParameterizedTest(name = "{0}")
                @MethodSource("filterCombinations")
                void successfullyFetchesTransactionsGivenQueryParameters(
                                String scenario,
                                boolean filterCategory,
                                boolean filterNecessity,
                                boolean filterStartDate,
                                boolean filterEndDate,
                                String expectedLabels) throws IOException {

                        TestFixture fixture = createFixture(
                                        "filters-"
                                                        + scenario.replace(' ', '-')
                                                        + ".db");

                        DatabaseConnectionManager connectionManager = new DatabaseConnectionManager(
                                        fixture.dataSource());

                        Category secondaryCategory = new SQLiteCategoryRepository(connectionManager).save(
                                        Category.builder()
                                                        .name("Secondary Expense")
                                                        .type(TransactionType.EXPENSE)
                                                        .build());

                        // Save order deliberately differs from date order.
                        FinancialTransaction a = insertExpense(
                                        fixture, fixture.expenseCategoryId(),
                                        Necessity.NECESSARY, NOV_05, "A");

                        FinancialTransaction b = insertExpense(
                                        fixture, fixture.expenseCategoryId(),
                                        Necessity.UNNECESSARY, NOV_05, "B");

                        FinancialTransaction c = insertExpense(
                                        fixture, secondaryCategory.getId(),
                                        Necessity.NECESSARY, NOV_05, "C");

                        FinancialTransaction d = insertExpense(
                                        fixture, fixture.expenseCategoryId(),
                                        Necessity.NECESSARY, NOV_03, "D");

                        FinancialTransaction e = insertExpense(
                                        fixture, fixture.expenseCategoryId(),
                                        Necessity.NECESSARY, NOV_07, "E");

                        FinancialTransaction f = insertExpense(
                                        fixture, fixture.expenseCategoryId(),
                                        Necessity.NECESSARY, NOV_09, "F");

                        Map<String, FinancialTransaction> transactions = Map.of(
                                        "A", a,
                                        "B", b,
                                        "C", c,
                                        "D", d,
                                        "E", e,
                                        "F", f);

                        FinancialTransactionQuery query = FinancialTransactionQuery.builder()
                                        .profileId(fixture.profileId())
                                        .type(TransactionType.EXPENSE)
                                        .categoryId(filterCategory
                                                        ? fixture.expenseCategoryId()
                                                        : null)
                                        .necessity(filterNecessity
                                                        ? Necessity.NECESSARY
                                                        : null)
                                        .startDate(filterStartDate ? NOV_05 : null)
                                        .endDate(filterEndDate ? NOV_05 : null)
                                        .build();

                        Page<FinancialTransaction> result = fixture.repository().findAll(
                                        query,
                                        pageRequest(20, null));

                        List<Long> expectedIds = Stream.of(
                                        expectedLabels.split(","))
                                        .map(label -> transactions.get(label).getId())
                                        .toList();

                        List<Long> actualIds = result.getContent().stream()
                                        .map(FinancialTransaction::getId)
                                        .toList();

                        assertEquals(expectedIds, actualIds);
                        assertEquals(20, result.getPageSize());
                        assertFalse(result.isHasNext());
                        assertNull(result.getNextPageToken());
                }

                @Test
                void successfullyReturnsOnlyIncomeTransactions()
                                throws IOException {

                        TestFixture fixture = createFixture(
                                        "filter-income.db");

                        FinancialTransaction expense = insertExpense(
                                        fixture,
                                        fixture.expenseCategoryId(),
                                        Necessity.NECESSARY,
                                        NOV_05,
                                        "Expense");

                        FinancialTransaction income = fixture.repository().save(
                                        createIncomeTransaction(
                                                        fixture.profileId(),
                                                        fixture.incomeCategoryId()));

                        FinancialTransactionQuery query = FinancialTransactionQuery.builder()
                                        .profileId(fixture.profileId())
                                        .type(TransactionType.INCOME)
                                        .build();

                        Page<FinancialTransaction> result = fixture.repository().findAll(
                                        query,
                                        pageRequest(10, null));

                        assertEquals(List.of(income), result.getContent());
                        assertFalse(result.getContent().contains(expense));
                        assertNull(result.getContent().get(0).getNecessity());
                }

                @Test
                void successfullyReturnsOnlyTransactionsBelongingToRequestedProfile()
                                throws IOException {

                        TestFixture fixture = createFixture(
                                        "filter-profile.db");

                        DatabaseConnectionManager connectionManager = new DatabaseConnectionManager(
                                        fixture.dataSource());

                        User otherUser = new SQLiteUserRepository(connectionManager).save(
                                        User.builder()
                                                        .firstName("otherFirstName")
                                                        .build());

                        ExpenseProfile otherProfile = new SQLiteExpenseProfileRepository(
                                        connectionManager).save(
                                                        ExpenseProfile.builder()
                                                                        .userId(otherUser.getId())
                                                                        .profileName("Other Profile")
                                                                        .build());

                        FinancialTransaction matchingTransaction = insertExpense(
                                        fixture,
                                        fixture.expenseCategoryId(),
                                        Necessity.NECESSARY,
                                        NOV_05,
                                        "Matching profile");

                        FinancialTransaction otherTransaction = fixture.repository().save(
                                        FinancialTransaction.builder()
                                                        .profileId(otherProfile.getId())
                                                        .categoryId(fixture.expenseCategoryId())
                                                        .type(TransactionType.EXPENSE)
                                                        .amount(AMOUNT)
                                                        .transactionDate(NOV_07)
                                                        .necessity(Necessity.NECESSARY)
                                                        .build());

                        Page<FinancialTransaction> result = fixture.repository().findAll(
                                        expenseQuery(fixture.profileId()),
                                        pageRequest(10, null));

                        assertEquals(
                                        List.of(matchingTransaction),
                                        result.getContent());

                        assertFalse(result.getContent().contains(otherTransaction));
                }

                @Test
                void successfullyPaginatesTransactionsAcrossMultiplePages()
                                throws IOException {

                        TestFixture fixture = createFixture(
                                        "pagination-multiple-pages.db");

                        // IDs increase in insertion order, not transaction-date order.
                        FinancialTransaction oldest = insertExpense(
                                        fixture, fixture.expenseCategoryId(),
                                        Necessity.NECESSARY, NOV_05, "Oldest");

                        FinancialTransaction sameDateLowerId = insertExpense(
                                        fixture, fixture.expenseCategoryId(),
                                        Necessity.NECESSARY, NOV_08, "Same date lower ID");

                        FinancialTransaction sameDateHigherId = insertExpense(
                                        fixture, fixture.expenseCategoryId(),
                                        Necessity.NECESSARY, NOV_08, "Same date higher ID");

                        FinancialTransaction middle = insertExpense(
                                        fixture, fixture.expenseCategoryId(),
                                        Necessity.NECESSARY, NOV_07, "Middle");

                        FinancialTransaction newerThanOldest = insertExpense(
                                        fixture, fixture.expenseCategoryId(),
                                        Necessity.NECESSARY, NOV_06, "Newer than oldest");

                        FinancialTransactionQuery query = expenseQuery(fixture.profileId());

                        Page<FinancialTransaction> firstPage = fixture.repository().findAll(
                                        query,
                                        pageRequest(2, null));

                        assertEquals(
                                        List.of(sameDateHigherId, sameDateLowerId),
                                        firstPage.getContent());

                        assertTrue(firstPage.isHasNext());
                        assertEquals(
                                        NOV_08 + ":" + sameDateLowerId.getId(),
                                        firstPage.getNextPageToken());

                        Page<FinancialTransaction> secondPage = fixture.repository().findAll(
                                        query,
                                        pageRequest(2, firstPage.getNextPageToken()));

                        assertEquals(
                                        List.of(middle, newerThanOldest),
                                        secondPage.getContent());

                        assertTrue(secondPage.isHasNext());
                        assertEquals(
                                        NOV_06 + ":" + newerThanOldest.getId(),
                                        secondPage.getNextPageToken());

                        Page<FinancialTransaction> thirdPage = fixture.repository().findAll(
                                        query,
                                        pageRequest(2, secondPage.getNextPageToken()));

                        assertEquals(List.of(oldest), thirdPage.getContent());
                        assertFalse(thirdPage.isHasNext());
                        assertNull(thirdPage.getNextPageToken());

                        List<FinancialTransaction> allRetrieved = new ArrayList<>();

                        allRetrieved.addAll(firstPage.getContent());
                        allRetrieved.addAll(secondPage.getContent());
                        allRetrieved.addAll(thirdPage.getContent());

                        assertEquals(5, allRetrieved.size());

                        Set<Long> uniqueIds = new HashSet<>();

                        for (FinancialTransaction transaction : allRetrieved) {
                                assertTrue(uniqueIds.add(transaction.getId()));
                        }

                        assertEquals(5, uniqueIds.size());
                }

                @ParameterizedTest
                @ValueSource(ints = { 0, 2, 3 })
                void successfullyHandlesPageSizeBoundaries(
                                int matchingTransactionCount) throws IOException {

                        TestFixture fixture = createFixture(
                                        "pagination-boundary-"
                                                        + matchingTransactionCount + ".db");

                        for (int i = 0; i < matchingTransactionCount; i++) {
                                insertExpense(
                                                fixture,
                                                fixture.expenseCategoryId(),
                                                Necessity.NECESSARY,
                                                NOV_05.plusDays(i),
                                                "Boundary " + i);
                        }

                        Page<FinancialTransaction> result = fixture.repository().findAll(
                                        expenseQuery(fixture.profileId()),
                                        pageRequest(2, null));

                        assertEquals(
                                        Math.min(matchingTransactionCount, 2),
                                        result.getContent().size());

                        boolean expectedHasNext = matchingTransactionCount > 2;

                        assertEquals(expectedHasNext, result.isHasNext());

                        if (expectedHasNext) {
                                FinancialTransaction last = result.getContent().get(1);

                                assertEquals(
                                                last.getTransactionDate() + ":" + last.getId(),
                                                result.getNextPageToken());
                        } else {
                                assertNull(result.getNextPageToken());
                        }
                }

                @Test
                void successfullyPaginatesOnlyTransactionsMatchingFilters()
                                throws IOException {

                        TestFixture fixture = createFixture(
                                        "pagination-filtered.db");

                        FinancialTransaction necessaryOldest = insertExpense(
                                        fixture, fixture.expenseCategoryId(),
                                        Necessity.NECESSARY, NOV_05, "Necessary oldest");

                        insertExpense(
                                        fixture, fixture.expenseCategoryId(),
                                        Necessity.UNNECESSARY, NOV_06, "Unnecessary");

                        FinancialTransaction necessaryMiddle = insertExpense(
                                        fixture, fixture.expenseCategoryId(),
                                        Necessity.NECESSARY, NOV_07, "Necessary middle");

                        insertExpense(
                                        fixture, fixture.expenseCategoryId(),
                                        Necessity.UNNECESSARY, NOV_08, "Unnecessary newest");

                        FinancialTransaction necessaryNewest = insertExpense(
                                        fixture, fixture.expenseCategoryId(),
                                        Necessity.NECESSARY, NOV_09, "Necessary newest");

                        FinancialTransactionQuery query = FinancialTransactionQuery.builder()
                                        .profileId(fixture.profileId())
                                        .type(TransactionType.EXPENSE)
                                        .necessity(Necessity.NECESSARY)
                                        .build();

                        Page<FinancialTransaction> firstPage = fixture.repository().findAll(
                                        query,
                                        pageRequest(2, null));

                        assertEquals(
                                        List.of(necessaryNewest, necessaryMiddle),
                                        firstPage.getContent());

                        assertTrue(firstPage.isHasNext());

                        Page<FinancialTransaction> secondPage = fixture.repository().findAll(
                                        query,
                                        pageRequest(2, firstPage.getNextPageToken()));

                        assertEquals(
                                        List.of(necessaryOldest),
                                        secondPage.getContent());

                        assertFalse(secondPage.isHasNext());
                        assertNull(secondPage.getNextPageToken());
                }

                @Test
                void throwsPersistenceExceptionWhenGetConnectionThrowsSQLException()
                                throws SQLException {

                        DatabaseConnectionManager connectionManager = mock(DatabaseConnectionManager.class);

                        SQLException expectedException = new SQLException("Connection failed");

                        when(connectionManager.getConnection())
                                        .thenThrow(expectedException);

                        SQLiteFinancialTransactionRepository repository = new SQLiteFinancialTransactionRepository(
                                        connectionManager);

                        PersistenceException actualException = assertThrows(
                                        PersistenceException.class,
                                        () -> repository.findAll(
                                                        expenseQuery(1L),
                                                        pageRequest(10, null)));

                        assertSame(expectedException, actualException.getCause());
                        assertEquals(
                                        "Unable to find transactions",
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

                        SQLiteFinancialTransactionRepository repository = new SQLiteFinancialTransactionRepository(
                                        connectionManager);

                        PersistenceException actualException = assertThrows(
                                        PersistenceException.class,
                                        () -> repository.findAll(
                                                        expenseQuery(1L),
                                                        pageRequest(10, null)));

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

                        SQLiteFinancialTransactionRepository repository = new SQLiteFinancialTransactionRepository(
                                        jdbc.connectionManager());

                        PersistenceException actualException = assertThrows(
                                        PersistenceException.class,
                                        () -> repository.findAll(
                                                        expenseQuery(1L),
                                                        pageRequest(10, null)));

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

                        SQLiteFinancialTransactionRepository repository = new SQLiteFinancialTransactionRepository(
                                        jdbc.connectionManager());

                        PersistenceException actualException = assertThrows(
                                        PersistenceException.class,
                                        () -> repository.findAll(
                                                        expenseQuery(1L),
                                                        pageRequest(10, null)));

                        assertSame(expectedException, actualException.getCause());

                        verify(jdbc.resultSet()).close();
                        verify(jdbc.statement()).close();
                        verify(jdbc.connection()).close();
                }

                @Test
                void throwsPersistenceExceptionWhenTransactionColumnsCannotBeRead()
                                throws SQLException {

                        JdbcMocks jdbc = createJdbcMocks();

                        SQLException expectedException = new SQLException("Unable to read transaction column");

                        when(jdbc.resultSet().next()).thenReturn(true);

                        when(jdbc.resultSet().getLong(TRANSACTION_ID_COLUMN))
                                        .thenThrow(expectedException);

                        SQLiteFinancialTransactionRepository repository = new SQLiteFinancialTransactionRepository(
                                        jdbc.connectionManager());

                        PersistenceException actualException = assertThrows(
                                        PersistenceException.class,
                                        () -> repository.findAll(
                                                        expenseQuery(1L),
                                                        pageRequest(10, null)));

                        assertSame(expectedException, actualException.getCause());

                        verify(jdbc.resultSet()).close();
                        verify(jdbc.statement()).close();
                        verify(jdbc.connection()).close();
                }

                @Test
                void closesJdbcResourcesWhenQueryReturnsNoTransactions()
                                throws SQLException {

                        JdbcMocks jdbc = createJdbcMocks();

                        when(jdbc.resultSet().next()).thenReturn(false);

                        SQLiteFinancialTransactionRepository repository = new SQLiteFinancialTransactionRepository(
                                        jdbc.connectionManager());

                        Page<FinancialTransaction> result = repository.findAll(
                                        expenseQuery(1L),
                                        pageRequest(10, null));

                        assertTrue(result.getContent().isEmpty());
                        assertFalse(result.isHasNext());
                        assertNull(result.getNextPageToken());

                        verify(jdbc.statement()).setLong(1, 1L);
                        verify(jdbc.statement()).setString(
                                        2, TransactionType.EXPENSE.name());
                        verify(jdbc.statement()).setInt(3, 11);

                        verify(jdbc.resultSet()).close();
                        verify(jdbc.statement()).close();
                        verify(jdbc.connection()).close();
                }
        }

        @Nested
        @DisplayName("Tests for updating financial transactions")
        class UpdateTests {

                @Test
                void throwsNullPointerExceptionWhenTransactionIsNull() {
                        DatabaseConnectionManager connectionManager = mock(DatabaseConnectionManager.class);

                        SQLiteFinancialTransactionRepository repository = new SQLiteFinancialTransactionRepository(
                                        connectionManager);

                        assertThrows(
                                        NullPointerException.class,
                                        () -> repository.update(null));

                        verifyNoInteractions(connectionManager);
                }

                @Test
                void throwsIllegalArgumentExceptionWhenTransactionIdIsNull() {
                        DatabaseConnectionManager connectionManager = mock(DatabaseConnectionManager.class);

                        SQLiteFinancialTransactionRepository repository = new SQLiteFinancialTransactionRepository(
                                        connectionManager);

                        FinancialTransaction transaction = createExpenseTransaction(1L, 1L);

                        assertThrows(
                                        IllegalArgumentException.class,
                                        () -> repository.update(transaction));

                        verifyNoInteractions(connectionManager);
                }

                @Test
                void successfullyUpdatesTransactionFieldsAndPersistsChanges()
                                throws IOException {

                        TestFixture fixture = createFixture(
                                        "update-transaction-fields.db");

                        FinancialTransaction original = fixture.repository().save(
                                        createExpenseTransaction(
                                                        fixture.profileId(),
                                                        fixture.expenseCategoryId()));

                        LocalDate updatedDate = TRANSACTION_DATE.plusDays(5);
                        long updatedAmount = AMOUNT + 500L;
                        String updatedDescription = "Updated purchase";

                        FinancialTransaction modified = FinancialTransaction.reconstitute(
                                        original.getId(),
                                        original.getProfileId(),
                                        original.getCategoryId(),
                                        updatedAmount,
                                        TransactionType.EXPENSE,
                                        updatedDate,
                                        updatedDescription,
                                        Necessity.UNNECESSARY);

                        FinancialTransaction updated = fixture.repository().update(modified);

                        assertEquals(original.getId(), updated.getId());
                        assertEquals(original.getProfileId(), updated.getProfileId());
                        assertEquals(original.getCategoryId(), updated.getCategoryId());
                        assertEquals(updatedAmount, updated.getAmount());
                        assertEquals(updatedDate, updated.getTransactionDate());
                        assertEquals(updatedDescription, updated.getDescription());
                        assertEquals(Necessity.UNNECESSARY, updated.getNecessity());
                        assertEquals(TransactionType.EXPENSE, updated.getType());

                        assertEquals(
                                        modified,
                                        fixture.repository()
                                                        .findById(original.getId())
                                                        .orElseThrow());

                        // Updating the database must not mutate the original object.
                        assertEquals(AMOUNT, original.getAmount());
                        assertEquals(DESCRIPTION, original.getDescription());
                        assertEquals(Necessity.NECESSARY, original.getNecessity());
                }

                @Test
                void successfullyUpdatesTransactionWithNullDescription()
                                throws IOException {

                        TestFixture fixture = createFixture(
                                        "update-null-description.db");

                        FinancialTransaction original = fixture.repository().save(
                                        createExpenseTransaction(
                                                        fixture.profileId(),
                                                        fixture.expenseCategoryId()));

                        FinancialTransaction modified = FinancialTransaction.reconstitute(
                                        original.getId(),
                                        original.getProfileId(),
                                        original.getCategoryId(),
                                        original.getAmount(),
                                        original.getType(),
                                        original.getTransactionDate(),
                                        null,
                                        original.getNecessity());

                        FinancialTransaction updated = fixture.repository().update(modified);

                        assertNull(updated.getDescription());

                        FinancialTransaction persisted = fixture.repository()
                                        .findById(original.getId())
                                        .orElseThrow();

                        assertNull(persisted.getDescription());
                        assertEquals(modified, persisted);
                }

                @Test
                void successfullyChangesExpenseTransactionToIncomeTransaction()
                                throws IOException {

                        TestFixture fixture = createFixture(
                                        "update-expense-to-income.db");

                        FinancialTransaction original = fixture.repository().save(
                                        createExpenseTransaction(
                                                        fixture.profileId(),
                                                        fixture.expenseCategoryId()));

                        FinancialTransaction modified = FinancialTransaction.reconstitute(
                                        original.getId(),
                                        original.getProfileId(),
                                        fixture.incomeCategoryId(),
                                        original.getAmount(),
                                        TransactionType.INCOME,
                                        original.getTransactionDate(),
                                        null,
                                        null);

                        FinancialTransaction updated = fixture.repository().update(modified);

                        assertEquals(original.getId(), updated.getId());
                        assertEquals(fixture.incomeCategoryId(), updated.getCategoryId());
                        assertEquals(TransactionType.INCOME, updated.getType());
                        assertNull(updated.getDescription());
                        assertNull(updated.getNecessity());

                        assertEquals(
                                        modified,
                                        fixture.repository()
                                                        .findById(original.getId())
                                                        .orElseThrow());
                }

                @Test
                void successfullyChangesIncomeTransactionToExpenseTransaction()
                                throws IOException {

                        TestFixture fixture = createFixture(
                                        "update-income-to-expense.db");

                        FinancialTransaction original = fixture.repository().save(
                                        createIncomeTransaction(
                                                        fixture.profileId(),
                                                        fixture.incomeCategoryId()));

                        FinancialTransaction modified = FinancialTransaction.reconstitute(
                                        original.getId(),
                                        original.getProfileId(),
                                        fixture.expenseCategoryId(),
                                        original.getAmount(),
                                        TransactionType.EXPENSE,
                                        original.getTransactionDate(),
                                        DESCRIPTION,
                                        Necessity.NECESSARY);

                        FinancialTransaction updated = fixture.repository().update(modified);

                        assertEquals(original.getId(), updated.getId());
                        assertEquals(
                                        fixture.expenseCategoryId(),
                                        updated.getCategoryId());
                        assertEquals(TransactionType.EXPENSE, updated.getType());
                        assertEquals(Necessity.NECESSARY, updated.getNecessity());
                        assertEquals(DESCRIPTION, updated.getDescription());

                        assertEquals(
                                        modified,
                                        fixture.repository()
                                                        .findById(original.getId())
                                                        .orElseThrow());
                }

                @Test
                void successfullyChangesTransactionProfile()
                                throws IOException {

                        TestFixture fixture = createFixture(
                                        "update-transaction-profile.db");

                        DatabaseConnectionManager connectionManager = new DatabaseConnectionManager(
                                        fixture.dataSource());

                        User otherUser = new SQLiteUserRepository(connectionManager).save(
                                        User.builder()
                                                        .firstName("otherFirstName")
                                                        .build());

                        ExpenseProfile otherProfile = new SQLiteExpenseProfileRepository(connectionManager).save(
                                        ExpenseProfile.builder()
                                                        .userId(otherUser.getId())
                                                        .profileName("Other Profile")
                                                        .build());

                        FinancialTransaction original = fixture.repository().save(
                                        createExpenseTransaction(
                                                        fixture.profileId(),
                                                        fixture.expenseCategoryId()));

                        FinancialTransaction modified = FinancialTransaction.reconstitute(
                                        original.getId(),
                                        otherProfile.getId(),
                                        original.getCategoryId(),
                                        original.getAmount(),
                                        original.getType(),
                                        original.getTransactionDate(),
                                        original.getDescription(),
                                        original.getNecessity());

                        FinancialTransaction updated = fixture.repository().update(modified);

                        assertEquals(otherProfile.getId(), updated.getProfileId());

                        assertEquals(
                                        modified,
                                        fixture.repository()
                                                        .findById(original.getId())
                                                        .orElseThrow());

                        Page<FinancialTransaction> oldProfileResults = fixture.repository().findAll(
                                        FinancialTransactionQuery.builder()
                                                        .profileId(fixture.profileId())
                                                        .type(TransactionType.EXPENSE)
                                                        .build(),
                                        PageRequest.builder()
                                                        .pageSize(10)
                                                        .build());

                        assertTrue(oldProfileResults.getContent().isEmpty());

                        Page<FinancialTransaction> newProfileResults = fixture.repository().findAll(
                                        FinancialTransactionQuery.builder()
                                                        .profileId(otherProfile.getId())
                                                        .type(TransactionType.EXPENSE)
                                                        .build(),
                                        PageRequest.builder()
                                                        .pageSize(10)
                                                        .build());

                        assertEquals(
                                        List.of(updated),
                                        newProfileResults.getContent());
                }

                @Test
                void throwsIllegalArgumentExceptionWhenTransactionDoesNotExist()
                                throws IOException {

                        TestFixture fixture = createFixture(
                                        "update-missing-transaction.db");

                        long nonexistentTransactionId = 9999L;

                        FinancialTransaction transaction = FinancialTransaction.reconstitute(
                                        nonexistentTransactionId,
                                        fixture.profileId(),
                                        fixture.expenseCategoryId(),
                                        AMOUNT,
                                        TransactionType.EXPENSE,
                                        TRANSACTION_DATE,
                                        DESCRIPTION,
                                        Necessity.NECESSARY);

                        IllegalArgumentException actualException = assertThrows(
                                        IllegalArgumentException.class,
                                        () -> fixture.repository().update(transaction));

                        assertEquals(
                                        "FinancialTransaction does not exist with id: "
                                                        + nonexistentTransactionId,
                                        actualException.getMessage());

                        assertTrue(
                                        fixture.repository()
                                                        .findById(nonexistentTransactionId)
                                                        .isEmpty());
                }

                @Test
                void throwsPersistenceExceptionWhenCategoryTypeDoesNotMatchTransactionType()
                                throws IOException {

                        TestFixture fixture = createFixture(
                                        "update-mismatched-category-type.db");

                        FinancialTransaction original = fixture.repository().save(
                                        createExpenseTransaction(
                                                        fixture.profileId(),
                                                        fixture.expenseCategoryId()));

                        // Income transactions cannot reference an expense category.
                        FinancialTransaction invalidUpdate = FinancialTransaction.reconstitute(
                                        original.getId(),
                                        original.getProfileId(),
                                        fixture.expenseCategoryId(),
                                        original.getAmount(),
                                        TransactionType.INCOME,
                                        original.getTransactionDate(),
                                        null,
                                        null);

                        PersistenceException actualException = assertThrows(
                                        PersistenceException.class,
                                        () -> fixture.repository().update(invalidUpdate));

                        assertTrue(actualException.getCause() instanceof SQLException);

                        assertEquals(
                                        original,
                                        fixture.repository()
                                                        .findById(original.getId())
                                                        .orElseThrow());
                }

                @Test
                void throwsPersistenceExceptionWhenUpdatedCategoryDoesNotExist()
                                throws IOException {

                        TestFixture fixture = createFixture(
                                        "update-missing-category.db");

                        FinancialTransaction original = fixture.repository().save(
                                        createExpenseTransaction(
                                                        fixture.profileId(),
                                                        fixture.expenseCategoryId()));

                        FinancialTransaction invalidUpdate = FinancialTransaction.reconstitute(
                                        original.getId(),
                                        original.getProfileId(),
                                        9999L,
                                        original.getAmount(),
                                        original.getType(),
                                        original.getTransactionDate(),
                                        original.getDescription(),
                                        original.getNecessity());

                        PersistenceException actualException = assertThrows(
                                        PersistenceException.class,
                                        () -> fixture.repository().update(invalidUpdate));

                        assertTrue(actualException.getCause() instanceof SQLException);

                        assertEquals(
                                        original,
                                        fixture.repository()
                                                        .findById(original.getId())
                                                        .orElseThrow());
                }

                @Test
                void throwsPersistenceExceptionWhenUpdatedProfileDoesNotExist()
                                throws IOException {

                        TestFixture fixture = createFixture(
                                        "update-missing-profile.db");

                        FinancialTransaction original = fixture.repository().save(
                                        createExpenseTransaction(
                                                        fixture.profileId(),
                                                        fixture.expenseCategoryId()));

                        FinancialTransaction invalidUpdate = FinancialTransaction.reconstitute(
                                        original.getId(),
                                        9999L,
                                        original.getCategoryId(),
                                        original.getAmount(),
                                        original.getType(),
                                        original.getTransactionDate(),
                                        original.getDescription(),
                                        original.getNecessity());

                        PersistenceException actualException = assertThrows(
                                        PersistenceException.class,
                                        () -> fixture.repository().update(invalidUpdate));

                        assertTrue(actualException.getCause() instanceof SQLException);

                        assertEquals(
                                        original,
                                        fixture.repository()
                                                        .findById(original.getId())
                                                        .orElseThrow());
                }

                @Test
                void throwsPersistenceExceptionWhenGetConnectionThrowsSQLException()
                                throws SQLException {

                        DatabaseConnectionManager connectionManager = mock(DatabaseConnectionManager.class);

                        SQLException expectedException = new SQLException("Connection failed");

                        when(connectionManager.getConnection())
                                        .thenThrow(expectedException);

                        SQLiteFinancialTransactionRepository repository = new SQLiteFinancialTransactionRepository(
                                        connectionManager);

                        FinancialTransaction transaction = FinancialTransaction.reconstitute(
                                        TRANSACTION_ID,
                                        1L,
                                        1L,
                                        AMOUNT,
                                        TransactionType.EXPENSE,
                                        TRANSACTION_DATE,
                                        DESCRIPTION,
                                        Necessity.NECESSARY);

                        PersistenceException actualException = assertThrows(
                                        PersistenceException.class,
                                        () -> repository.update(transaction));

                        assertSame(expectedException, actualException.getCause());
                        assertEquals(
                                        "Unable to update financial transaction",
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

                        SQLiteFinancialTransactionRepository repository = new SQLiteFinancialTransactionRepository(
                                        connectionManager);

                        FinancialTransaction transaction = FinancialTransaction.reconstitute(
                                        TRANSACTION_ID,
                                        1L,
                                        1L,
                                        AMOUNT,
                                        TransactionType.EXPENSE,
                                        TRANSACTION_DATE,
                                        DESCRIPTION,
                                        Necessity.NECESSARY);

                        PersistenceException actualException = assertThrows(
                                        PersistenceException.class,
                                        () -> repository.update(transaction));

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

                        SQLiteFinancialTransactionRepository repository = new SQLiteFinancialTransactionRepository(
                                        jdbc.connectionManager());

                        FinancialTransaction transaction = FinancialTransaction.reconstitute(
                                        TRANSACTION_ID,
                                        1L,
                                        1L,
                                        AMOUNT,
                                        TransactionType.EXPENSE,
                                        TRANSACTION_DATE,
                                        DESCRIPTION,
                                        Necessity.NECESSARY);

                        PersistenceException actualException = assertThrows(
                                        PersistenceException.class,
                                        () -> repository.update(transaction));

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

                        SQLiteFinancialTransactionRepository repository = new SQLiteFinancialTransactionRepository(
                                        jdbc.connectionManager());

                        FinancialTransaction transaction = FinancialTransaction.reconstitute(
                                        TRANSACTION_ID,
                                        1L,
                                        1L,
                                        AMOUNT,
                                        TransactionType.EXPENSE,
                                        TRANSACTION_DATE,
                                        DESCRIPTION,
                                        Necessity.NECESSARY);

                        PersistenceException actualException = assertThrows(
                                        PersistenceException.class,
                                        () -> repository.update(transaction));

                        assertSame(expectedException, actualException.getCause());

                        verify(jdbc.resultSet()).close();
                        verify(jdbc.statement()).close();
                        verify(jdbc.connection()).close();
                }

                @Test
                void throwsPersistenceExceptionWhenTransactionColumnsCannotBeRead()
                                throws SQLException {

                        JdbcMocks jdbc = createJdbcMocks();

                        SQLException expectedException = new SQLException("Unable to read updated transaction");

                        when(jdbc.resultSet().next()).thenReturn(true);

                        when(jdbc.resultSet().getLong(TRANSACTION_ID_COLUMN))
                                        .thenThrow(expectedException);

                        SQLiteFinancialTransactionRepository repository = new SQLiteFinancialTransactionRepository(
                                        jdbc.connectionManager());

                        FinancialTransaction transaction = FinancialTransaction.reconstitute(
                                        TRANSACTION_ID,
                                        1L,
                                        1L,
                                        AMOUNT,
                                        TransactionType.EXPENSE,
                                        TRANSACTION_DATE,
                                        DESCRIPTION,
                                        Necessity.NECESSARY);

                        PersistenceException actualException = assertThrows(
                                        PersistenceException.class,
                                        () -> repository.update(transaction));

                        assertSame(expectedException, actualException.getCause());

                        verify(jdbc.resultSet()).close();
                        verify(jdbc.statement()).close();
                        verify(jdbc.connection()).close();
                }

                @Test
                void closesJdbcResourcesAndBindsParametersWhenUpdateSucceeds()
                                throws SQLException {

                        JdbcMocks jdbc = createJdbcMocks();

                        when(jdbc.resultSet().next()).thenReturn(true);
                        when(jdbc.resultSet().getLong(TRANSACTION_ID_COLUMN))
                                        .thenReturn(TRANSACTION_ID);
                        when(jdbc.resultSet().getLong(PROFILE_ID_COLUMN))
                                        .thenReturn(1L);
                        when(jdbc.resultSet().getLong(CATEGORY_ID_COLUMN))
                                        .thenReturn(2L);
                        when(jdbc.resultSet().getString(TYPE_COLUMN))
                                        .thenReturn(TransactionType.INCOME.name());
                        when(jdbc.resultSet().getLong(AMOUNT_COLUMN))
                                        .thenReturn(AMOUNT);
                        when(jdbc.resultSet().getString(DESCRIPTION_COLUMN))
                                        .thenReturn(null);
                        when(jdbc.resultSet().getString(TRANSACTION_DATE_COLUMN))
                                        .thenReturn(TRANSACTION_DATE.toString());
                        when(jdbc.resultSet().getString(NECESSITY_COLUMN))
                                        .thenReturn(null);

                        SQLiteFinancialTransactionRepository repository = new SQLiteFinancialTransactionRepository(
                                        jdbc.connectionManager());

                        FinancialTransaction transaction = FinancialTransaction.reconstitute(
                                        TRANSACTION_ID,
                                        1L,
                                        2L,
                                        AMOUNT,
                                        TransactionType.INCOME,
                                        TRANSACTION_DATE,
                                        null,
                                        null);

                        FinancialTransaction updated = repository.update(transaction);

                        assertEquals(transaction, updated);

                        verify(jdbc.statement()).setLong(1, 1L);
                        verify(jdbc.statement()).setLong(2, 2L);
                        verify(jdbc.statement()).setString(
                                        3, TransactionType.INCOME.name());
                        verify(jdbc.statement()).setLong(4, AMOUNT);
                        verify(jdbc.statement()).setNull(5, Types.VARCHAR);
                        verify(jdbc.statement()).setString(
                                        6, TRANSACTION_DATE.toString());
                        verify(jdbc.statement()).setNull(7, Types.VARCHAR);
                        verify(jdbc.statement()).setLong(8, TRANSACTION_ID);

                        verify(jdbc.resultSet()).close();
                        verify(jdbc.statement()).close();
                        verify(jdbc.connection()).close();
                }
        }

        @Nested
        @DisplayName("Tests for deleting financial transactions")
        class DeleteByIdTests {

                @ParameterizedTest
                @ValueSource(longs = { 0L, -1L })
                void throwsIllegalArgumentExceptionWhenIdIsNotPositive(long id) {
                        DatabaseConnectionManager connectionManager = mock(DatabaseConnectionManager.class);

                        SQLiteFinancialTransactionRepository repository = new SQLiteFinancialTransactionRepository(
                                        connectionManager);

                        assertThrows(
                                        IllegalArgumentException.class,
                                        () -> repository.deleteById(id));

                        verifyNoInteractions(connectionManager);
                }

                @Test
                void successfullyDeletesExistingTransaction()
                                throws IOException {

                        TestFixture fixture = createFixture(
                                        "delete-existing-transaction.db");

                        FinancialTransaction savedTransaction = fixture.repository().save(
                                        createExpenseTransaction(
                                                        fixture.profileId(),
                                                        fixture.expenseCategoryId()));

                        boolean deleted = fixture.repository().deleteById(savedTransaction.getId());

                        assertTrue(deleted);

                        assertTrue(
                                        fixture.repository()
                                                        .findById(savedTransaction.getId())
                                                        .isEmpty());
                }

                @Test
                void returnsFalseWhenTransactionDoesNotExist()
                                throws IOException {

                        TestFixture fixture = createFixture(
                                        "delete-missing-transaction.db");

                        boolean deleted = fixture.repository().deleteById(9999L);

                        assertFalse(deleted);
                }

                @Test
                void successfullyDeletesOnlySpecifiedTransaction()
                                throws IOException {

                        TestFixture fixture = createFixture(
                                        "delete-only-specified-transaction.db");

                        FinancialTransaction firstTransaction = fixture.repository().save(
                                        createExpenseTransaction(
                                                        fixture.profileId(),
                                                        fixture.expenseCategoryId()));

                        FinancialTransaction secondTransaction = fixture.repository().save(
                                        createIncomeTransaction(
                                                        fixture.profileId(),
                                                        fixture.incomeCategoryId()));

                        assertTrue(
                                        fixture.repository()
                                                        .deleteById(firstTransaction.getId()));

                        assertTrue(
                                        fixture.repository()
                                                        .findById(firstTransaction.getId())
                                                        .isEmpty());

                        assertEquals(
                                        secondTransaction,
                                        fixture.repository()
                                                        .findById(secondTransaction.getId())
                                                        .orElseThrow());

                        // Deleting a transaction must not delete its category.
                        DatabaseConnectionManager connectionManager = new DatabaseConnectionManager(
                                        fixture.dataSource());

                        SQLiteCategoryRepository categoryRepository = new SQLiteCategoryRepository(connectionManager);

                        assertTrue(
                                        categoryRepository
                                                        .findById(fixture.expenseCategoryId())
                                                        .isPresent());
                }

                @Test
                void throwsPersistenceExceptionWhenGetConnectionThrowsSQLException()
                                throws SQLException {

                        DatabaseConnectionManager connectionManager = mock(DatabaseConnectionManager.class);

                        SQLException expectedException = new SQLException("Connection failed");

                        when(connectionManager.getConnection())
                                        .thenThrow(expectedException);

                        SQLiteFinancialTransactionRepository repository = new SQLiteFinancialTransactionRepository(
                                        connectionManager);

                        PersistenceException actualException = assertThrows(
                                        PersistenceException.class,
                                        () -> repository.deleteById(TRANSACTION_ID));

                        assertSame(expectedException, actualException.getCause());

                        assertEquals(
                                        "Unable to delete transaction",
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

                        SQLiteFinancialTransactionRepository repository = new SQLiteFinancialTransactionRepository(
                                        connectionManager);

                        PersistenceException actualException = assertThrows(
                                        PersistenceException.class,
                                        () -> repository.deleteById(TRANSACTION_ID));

                        assertSame(expectedException, actualException.getCause());

                        verify(connection).close();
                }

                @Test
                void throwsPersistenceExceptionWhenExecuteUpdateThrowsSQLException()
                                throws SQLException {

                        JdbcMocks jdbc = createJdbcMocks();

                        SQLException expectedException = new SQLException("Delete execution failed");

                        when(jdbc.statement().executeUpdate())
                                        .thenThrow(expectedException);

                        SQLiteFinancialTransactionRepository repository = new SQLiteFinancialTransactionRepository(
                                        jdbc.connectionManager());

                        PersistenceException actualException = assertThrows(
                                        PersistenceException.class,
                                        () -> repository.deleteById(TRANSACTION_ID));

                        assertSame(expectedException, actualException.getCause());

                        verify(jdbc.statement()).close();
                        verify(jdbc.connection()).close();
                }

                @Test
                void closesJdbcResourcesWhenDeleteSucceeds()
                                throws SQLException {

                        JdbcMocks jdbc = createJdbcMocks();

                        when(jdbc.statement().executeUpdate())
                                        .thenReturn(1);

                        SQLiteFinancialTransactionRepository repository = new SQLiteFinancialTransactionRepository(
                                        jdbc.connectionManager());

                        boolean deleted = repository.deleteById(TRANSACTION_ID);

                        assertTrue(deleted);

                        verify(jdbc.statement()).setLong(1, TRANSACTION_ID);
                        verify(jdbc.statement()).executeUpdate();

                        verify(jdbc.statement()).close();
                        verify(jdbc.connection()).close();
                }
        }
}
