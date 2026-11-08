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

import com.expensetracker.models.User;
import com.expensetracker.persistence.DatabaseConnectionManager;
import com.expensetracker.persistence.DatabaseMigrationManager;
import com.expensetracker.persistence.SQLiteDataSourceFactory;
import com.expensetracker.persistence.exceptions.PersistenceException;

/** Tests for {@link SQLiteUserRepository} */
class SQLiteUserRepositoryTest {

        private static final String USER_ID_COLUMN = "UserId";
        private static final String FIRST_NAME_COLUMN = "FirstName";
        private static final String LAST_NAME_COLUMN = "LastName";
        private static final String UPDATED_FIRST_NAME = "Updated";
        private static final String UPDATED_LAST_NAME = "User";
        private static final long NONEXISTENT_USER_ID = 9999L;

        private static final String SELECT_USER_BY_ID_SQL = String.format("""
                        SELECT %s, %s, %s
                        FROM User
                        WHERE %s = ?
                        """, USER_ID_COLUMN, FIRST_NAME_COLUMN, LAST_NAME_COLUMN, USER_ID_COLUMN);

        private static final long USER_ID = 1L;
        private static final String FIRST_NAME = "Sample";
        private static final String LAST_NAME = "Person";

        @TempDir
        Path tempDir;

        @Nested
        @DisplayName("Tests for SQLiteUserRepository constructor validation")
        class ConstructorTests {

                @Test
                void throwsNullPointerExceptionWhenConnectionManagerIsNull() {
                        assertThrows(
                                        NullPointerException.class,
                                        () -> new SQLiteUserRepository(null));
                }
        }

        @Nested
        @DisplayName("Tests for saving a new user, including validation, persistence, and JDBC error handling")
        class SaveTests {

                @Test
                void throwsNullPointerExceptionWhenUserIsNull() {
                        DatabaseConnectionManager connectionManager = mock(DatabaseConnectionManager.class);

                        SQLiteUserRepository repository = new SQLiteUserRepository(connectionManager);

                        assertThrows(
                                        NullPointerException.class,
                                        () -> repository.save(null));
                }

                @Test
                void throwsIllegalArgumentExceptionWhenUserIdIsNotNull() {
                        DatabaseConnectionManager connectionManager = mock(DatabaseConnectionManager.class);

                        SQLiteUserRepository repository = new SQLiteUserRepository(connectionManager);

                        User persistedUser = User.reconstitute(
                                        USER_ID, FIRST_NAME, LAST_NAME);

                        assertThrows(
                                        IllegalArgumentException.class,
                                        () -> repository.save(persistedUser));
                }

                @Test
                void insertSucceedsAndExpectedReconstitutedUserIsReturned()
                                throws IOException, SQLException {

                        DataSource dataSource = SQLiteDataSourceFactory.create(
                                        tempDir.resolve("test-expenses.db"));

                        new DatabaseMigrationManager(dataSource).migrate();

                        DatabaseConnectionManager connectionManager = new DatabaseConnectionManager(dataSource);

                        SQLiteUserRepository repository = new SQLiteUserRepository(connectionManager);

                        User user = createNewUser();

                        User savedUser = repository.save(user);

                        assertNotNull(savedUser.getId());
                        assertTrue(savedUser.getId() > 0);
                        assertEquals(FIRST_NAME, savedUser.getFirstName());
                        assertEquals(LAST_NAME, savedUser.getLastName());

                        // The original immutable user must remain unpersisted.
                        assertNull(user.getId());

                        // Verify the record exists in SQLite.
                        try (Connection connection = dataSource.getConnection();
                                        PreparedStatement statement = connection
                                                        .prepareStatement(SELECT_USER_BY_ID_SQL)) {

                                statement.setLong(1, savedUser.getId());

                                try (ResultSet resultSet = statement.executeQuery()) {
                                        assertTrue(resultSet.next());

                                        assertEquals(
                                                        savedUser.getId().longValue(),
                                                        resultSet.getLong(USER_ID_COLUMN));

                                        assertEquals(
                                                        FIRST_NAME,
                                                        resultSet.getString(FIRST_NAME_COLUMN));

                                        assertEquals(
                                                        LAST_NAME,
                                                        resultSet.getString(LAST_NAME_COLUMN));

                                        assertFalse(resultSet.next());
                                }
                        }
                }

                @Test
                void throwsPersistenceExceptionWhenGetConnectionFails()
                                throws SQLException {

                        DatabaseConnectionManager connectionManager = mock(DatabaseConnectionManager.class);

                        SQLException expectedException = new SQLException("Connection failed");

                        when(connectionManager.getConnection())
                                        .thenThrow(expectedException);

                        SQLiteUserRepository repository = new SQLiteUserRepository(connectionManager);

                        PersistenceException actualException = assertThrows(
                                        PersistenceException.class,
                                        () -> repository.save(createNewUser()));

                        assertSame(expectedException, actualException.getCause());
                        assertEquals(
                                        "Failed to save user",
                                        actualException.getMessage());
                }

                @Test
                void throwsPersistenceExceptionWhenPrepareStatementFails()
                                throws SQLException {

                        DatabaseConnectionManager connectionManager = mock(DatabaseConnectionManager.class);

                        Connection connection = mock(Connection.class);

                        SQLException expectedException = new SQLException("Statement preparation failed");

                        when(connectionManager.getConnection())
                                        .thenReturn(connection);

                        when(connection.prepareStatement(anyString()))
                                        .thenThrow(expectedException);

                        SQLiteUserRepository repository = new SQLiteUserRepository(connectionManager);

                        PersistenceException actualException = assertThrows(
                                        PersistenceException.class,
                                        () -> repository.save(createNewUser()));

                        assertSame(expectedException, actualException.getCause());
                        verify(connection).close();
                }

                @Test
                void throwsPersistenceExceptionWhenExecuteQueryFails()
                                throws SQLException {

                        JdbcMocks jdbc = createJdbcMocks();

                        SQLException expectedException = new SQLException("Insert execution failed");

                        when(jdbc.statement().executeQuery())
                                        .thenThrow(expectedException);

                        SQLiteUserRepository repository = new SQLiteUserRepository(jdbc.connectionManager());

                        PersistenceException actualException = assertThrows(
                                        PersistenceException.class,
                                        () -> repository.save(createNewUser()));

                        assertSame(expectedException, actualException.getCause());

                        verify(jdbc.statement()).close();
                        verify(jdbc.connection()).close();
                }

                @Test
                void throwsPersistenceExceptionWhenInsertReturnsNoGeneratedId()
                                throws SQLException {

                        JdbcMocks jdbc = createJdbcMocks();

                        when(jdbc.resultSet().next()).thenReturn(false);

                        SQLiteUserRepository repository = new SQLiteUserRepository(jdbc.connectionManager());

                        PersistenceException actualException = assertThrows(
                                        PersistenceException.class,
                                        () -> repository.save(createNewUser()));

                        assertNotNull(actualException.getCause());
                        assertEquals(
                                        "Insert did not return a generated user id",
                                        actualException.getCause().getMessage());

                        verify(jdbc.resultSet()).close();
                        verify(jdbc.statement()).close();
                        verify(jdbc.connection()).close();
                }

                @Test
                void throwsPersistenceExceptionWhenGeneratedIdCannotBeRead()
                                throws SQLException {

                        JdbcMocks jdbc = createJdbcMocks();

                        SQLException expectedException = new SQLException("Unable to read generated ID");

                        when(jdbc.resultSet().next()).thenReturn(true);
                        when(jdbc.resultSet().getLong(USER_ID_COLUMN))
                                        .thenThrow(expectedException);

                        SQLiteUserRepository repository = new SQLiteUserRepository(jdbc.connectionManager());

                        PersistenceException actualException = assertThrows(
                                        PersistenceException.class,
                                        () -> repository.save(createNewUser()));

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
                        when(jdbc.resultSet().getLong(USER_ID_COLUMN))
                                        .thenReturn(USER_ID);

                        SQLiteUserRepository repository = new SQLiteUserRepository(jdbc.connectionManager());

                        User savedUser = repository.save(createNewUser());

                        assertEquals(USER_ID, savedUser.getId().longValue());

                        verify(jdbc.resultSet()).close();
                        verify(jdbc.statement()).close();
                        verify(jdbc.connection()).close();
                }
        }

        @Nested
        @DisplayName("Tests for finding a user by ID, including validation, retrieval, and JDBC error handling")
        class FindByIdTests {

                @ParameterizedTest
                @ValueSource(longs = { 0L, -1L })
                void throwsIllegalArgumentExceptionWhenUserIdIsNotPositive(long id) {
                        DatabaseConnectionManager connectionManager = mock(DatabaseConnectionManager.class);

                        SQLiteUserRepository repository = new SQLiteUserRepository(connectionManager);

                        assertThrows(
                                        IllegalArgumentException.class,
                                        () -> repository.findById(id));

                        verifyNoInteractions(connectionManager);
                }

                @Test
                void successfullyReturnsUserWhenMatchIsFound()
                                throws IOException {

                        DataSource dataSource = SQLiteDataSourceFactory.create(
                                        tempDir.resolve("find-user.db"));

                        new DatabaseMigrationManager(dataSource).migrate();

                        DatabaseConnectionManager connectionManager = new DatabaseConnectionManager(dataSource);

                        SQLiteUserRepository repository = new SQLiteUserRepository(connectionManager);

                        User savedUser = repository.save(createNewUser());

                        Optional<User> result = repository.findById(savedUser.getId());

                        assertTrue(result.isPresent());
                        assertEquals(savedUser, result.get());
                        assertEquals(FIRST_NAME, result.get().getFirstName());
                        assertEquals(LAST_NAME, result.get().getLastName());
                }

                @Test
                void successfullyReturnsEmptyWhenNoMatchesAreFound()
                                throws IOException {

                        DataSource dataSource = SQLiteDataSourceFactory.create(
                                        tempDir.resolve("find-missing-user.db"));

                        new DatabaseMigrationManager(dataSource).migrate();

                        DatabaseConnectionManager connectionManager = new DatabaseConnectionManager(dataSource);

                        SQLiteUserRepository repository = new SQLiteUserRepository(connectionManager);

                        Optional<User> result = repository.findById(USER_ID);

                        assertTrue(result.isEmpty());
                }

                @Test
                void throwsPersistenceExceptionWhenGetConnectionFails()
                                throws SQLException {

                        DatabaseConnectionManager connectionManager = mock(DatabaseConnectionManager.class);

                        SQLException expectedException = new SQLException("Connection failed");

                        when(connectionManager.getConnection())
                                        .thenThrow(expectedException);

                        SQLiteUserRepository repository = new SQLiteUserRepository(connectionManager);

                        PersistenceException actualException = assertThrows(
                                        PersistenceException.class,
                                        () -> repository.findById(USER_ID));

                        assertSame(expectedException, actualException.getCause());
                }

                @Test
                void throwsPersistenceExceptionWhenPrepareStatementFails()
                                throws SQLException {

                        DatabaseConnectionManager connectionManager = mock(DatabaseConnectionManager.class);

                        Connection connection = mock(Connection.class);

                        SQLException expectedException = new SQLException("Statement preparation failed");

                        when(connectionManager.getConnection())
                                        .thenReturn(connection);

                        when(connection.prepareStatement(anyString()))
                                        .thenThrow(expectedException);

                        SQLiteUserRepository repository = new SQLiteUserRepository(connectionManager);

                        PersistenceException actualException = assertThrows(
                                        PersistenceException.class,
                                        () -> repository.findById(USER_ID));

                        assertSame(expectedException, actualException.getCause());
                        verify(connection).close();
                }

                @Test
                void throwsPersistenceExceptionWhenExecuteQueryFails()
                                throws SQLException {

                        JdbcMocks jdbc = createJdbcMocks();

                        SQLException expectedException = new SQLException("Query execution failed");

                        when(jdbc.statement().executeQuery())
                                        .thenThrow(expectedException);

                        SQLiteUserRepository repository = new SQLiteUserRepository(jdbc.connectionManager());

                        PersistenceException actualException = assertThrows(
                                        PersistenceException.class,
                                        () -> repository.findById(USER_ID));

                        assertSame(expectedException, actualException.getCause());

                        verify(jdbc.statement()).close();
                        verify(jdbc.connection()).close();
                }

                @Test
                void throwsPersistenceExceptionWhenResultSetNextFails()
                                throws SQLException {

                        JdbcMocks jdbc = createJdbcMocks();

                        SQLException expectedException = new SQLException("Unable to advance result set");

                        when(jdbc.resultSet().next())
                                        .thenThrow(expectedException);

                        SQLiteUserRepository repository = new SQLiteUserRepository(jdbc.connectionManager());

                        PersistenceException actualException = assertThrows(
                                        PersistenceException.class,
                                        () -> repository.findById(USER_ID));

                        assertSame(expectedException, actualException.getCause());

                        verify(jdbc.resultSet()).close();
                        verify(jdbc.statement()).close();
                        verify(jdbc.connection()).close();
                }

                @Test
                void throwsPersistenceExceptionWhenUserColumnsCannotBeRead()
                                throws SQLException {

                        JdbcMocks jdbc = createJdbcMocks();

                        SQLException expectedException = new SQLException("Unable to read user column");

                        when(jdbc.resultSet().next()).thenReturn(true);
                        when(jdbc.resultSet().getLong(USER_ID_COLUMN))
                                        .thenReturn(USER_ID);
                        when(jdbc.resultSet().getString(FIRST_NAME_COLUMN))
                                        .thenThrow(expectedException);

                        SQLiteUserRepository repository = new SQLiteUserRepository(jdbc.connectionManager());

                        PersistenceException actualException = assertThrows(
                                        PersistenceException.class,
                                        () -> repository.findById(USER_ID));

                        assertSame(expectedException, actualException.getCause());

                        verify(jdbc.resultSet()).close();
                        verify(jdbc.statement()).close();
                        verify(jdbc.connection()).close();
                }
        }

        @Nested
        @DisplayName("Tests for retrieving all users, including empty results, ordering, and JDBC error handling")
        class FindAllTests {

                @Test
                void successfullyReturnsEmptyListWhenNoUsersFound()
                                throws IOException {

                        DataSource dataSource = SQLiteDataSourceFactory.create(
                                        tempDir.resolve("find-all-empty.db"));

                        new DatabaseMigrationManager(dataSource).migrate();

                        DatabaseConnectionManager connectionManager = new DatabaseConnectionManager(dataSource);

                        SQLiteUserRepository repository = new SQLiteUserRepository(connectionManager);

                        List<User> users = repository.findAll();

                        assertNotNull(users);
                        assertTrue(users.isEmpty());
                }

                @Test
                void successfullyReturnsListOfUsersFoundOrderedById()
                                throws IOException {

                        DataSource dataSource = SQLiteDataSourceFactory.create(
                                        tempDir.resolve("find-all-users.db"));

                        new DatabaseMigrationManager(dataSource).migrate();

                        DatabaseConnectionManager connectionManager = new DatabaseConnectionManager(dataSource);

                        SQLiteUserRepository repository = new SQLiteUserRepository(connectionManager);

                        User firstUser = repository.save(User.builder()
                                        .firstName("First")
                                        .lastName("Person")
                                        .build());

                        User secondUser = repository.save(User.builder()
                                        .firstName("Second")
                                        .lastName("Person")
                                        .build());

                        User thirdUser = repository.save(User.builder()
                                        .firstName("Third")
                                        .lastName("Person")
                                        .build());

                        List<User> users = repository.findAll();

                        assertEquals(3, users.size());

                        assertEquals(firstUser, users.get(0));
                        assertEquals(secondUser, users.get(1));
                        assertEquals(thirdUser, users.get(2));

                        assertTrue(users.get(0).getId() < users.get(1).getId());
                        assertTrue(users.get(1).getId() < users.get(2).getId());
                }

                @Test
                void throwsPersistenceExceptionWhenGetConnectionThrowsSQLException()
                                throws SQLException {

                        DatabaseConnectionManager connectionManager = mock(DatabaseConnectionManager.class);

                        SQLException expectedException = new SQLException("Connection failed");

                        when(connectionManager.getConnection())
                                        .thenThrow(expectedException);

                        SQLiteUserRepository repository = new SQLiteUserRepository(connectionManager);

                        PersistenceException actualException = assertThrows(
                                        PersistenceException.class,
                                        repository::findAll);

                        assertSame(expectedException, actualException.getCause());
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

                        SQLiteUserRepository repository = new SQLiteUserRepository(connectionManager);

                        PersistenceException actualException = assertThrows(
                                        PersistenceException.class,
                                        repository::findAll);

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

                        SQLiteUserRepository repository = new SQLiteUserRepository(jdbc.connectionManager());

                        PersistenceException actualException = assertThrows(
                                        PersistenceException.class,
                                        repository::findAll);

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

                        SQLiteUserRepository repository = new SQLiteUserRepository(jdbc.connectionManager());

                        PersistenceException actualException = assertThrows(
                                        PersistenceException.class,
                                        repository::findAll);

                        assertSame(expectedException, actualException.getCause());

                        verify(jdbc.resultSet()).close();
                        verify(jdbc.statement()).close();
                        verify(jdbc.connection()).close();
                }

                @Test
                void throwsPersistenceExceptionWhenUserColumnsCannotBeRead()
                                throws SQLException {

                        JdbcMocks jdbc = createJdbcMocks();

                        SQLException expectedException = new SQLException("Unable to read user column");

                        when(jdbc.resultSet().next()).thenReturn(true);
                        when(jdbc.resultSet().getLong(USER_ID_COLUMN))
                                        .thenReturn(USER_ID);
                        when(jdbc.resultSet().getString(FIRST_NAME_COLUMN))
                                        .thenThrow(expectedException);

                        SQLiteUserRepository repository = new SQLiteUserRepository(jdbc.connectionManager());

                        PersistenceException actualException = assertThrows(
                                        PersistenceException.class,
                                        repository::findAll);

                        assertSame(expectedException, actualException.getCause());

                        verify(jdbc.resultSet()).close();
                        verify(jdbc.statement()).close();
                        verify(jdbc.connection()).close();
                }
        }

        @Nested
        @DisplayName("Tests for updating users, including validation, persistence, and JDBC error handling")
        class UpdateTests {

                @Test
                void throwsNullPointerExceptionWhenUserIsNull() {
                        DatabaseConnectionManager connectionManager = mock(DatabaseConnectionManager.class);

                        SQLiteUserRepository repository = new SQLiteUserRepository(connectionManager);

                        assertThrows(
                                        NullPointerException.class,
                                        () -> repository.update(null));

                        verifyNoInteractions(connectionManager);
                }

                @Test
                void throwsIllegalArgumentExceptionWhenUserIdIsNull() {
                        DatabaseConnectionManager connectionManager = mock(DatabaseConnectionManager.class);

                        SQLiteUserRepository repository = new SQLiteUserRepository(connectionManager);

                        assertThrows(
                                        IllegalArgumentException.class,
                                        () -> repository.update(createNewUser()));

                        verifyNoInteractions(connectionManager);
                }

                @Test
                void successfullyUpdatesUser() throws IOException {
                        DataSource dataSource = SQLiteDataSourceFactory.create(
                                        tempDir.resolve("update-user.db"));

                        new DatabaseMigrationManager(dataSource).migrate();

                        DatabaseConnectionManager connectionManager = new DatabaseConnectionManager(dataSource);

                        SQLiteUserRepository repository = new SQLiteUserRepository(connectionManager);

                        User originalUser = repository.save(createNewUser());

                        User userToUpdate = User.reconstitute(
                                        originalUser.getId(),
                                        UPDATED_FIRST_NAME,
                                        UPDATED_LAST_NAME);

                        User updatedUser = repository.update(userToUpdate);

                        // Verify the returned user.
                        assertEquals(originalUser.getId(), updatedUser.getId());
                        assertEquals(UPDATED_FIRST_NAME, updatedUser.getFirstName());
                        assertEquals(UPDATED_LAST_NAME, updatedUser.getLastName());

                        // Verify the updated values are persisted.
                        Optional<User> retrievedUser = repository.findById(originalUser.getId());

                        assertTrue(retrievedUser.isPresent());
                        assertEquals(updatedUser, retrievedUser.get());

                        // Verify the original immutable user remains unchanged.
                        assertEquals(FIRST_NAME, originalUser.getFirstName());
                        assertEquals(LAST_NAME, originalUser.getLastName());

                        // Verify the update did not insert another user.
                        List<User> users = repository.findAll();

                        assertEquals(1, users.size());
                        assertEquals(updatedUser, users.get(0));
                }

                @Test
                void throwsIllegalArgumentExceptionWhenNoUserUpdated()
                                throws IOException {

                        DataSource dataSource = SQLiteDataSourceFactory.create(
                                        tempDir.resolve("update-missing-user.db"));

                        new DatabaseMigrationManager(dataSource).migrate();

                        DatabaseConnectionManager connectionManager = new DatabaseConnectionManager(dataSource);

                        SQLiteUserRepository repository = new SQLiteUserRepository(connectionManager);

                        User nonexistentUser = User.reconstitute(
                                        NONEXISTENT_USER_ID,
                                        UPDATED_FIRST_NAME,
                                        UPDATED_LAST_NAME);

                        IllegalArgumentException actualException = assertThrows(
                                        IllegalArgumentException.class,
                                        () -> repository.update(nonexistentUser));

                        assertEquals(
                                        "User does not exist with id: " + NONEXISTENT_USER_ID,
                                        actualException.getMessage());

                        assertTrue(repository.findAll().isEmpty());
                }

                @Test
                void throwsPersistenceExceptionWhenGetConnectionThrowsSQLException()
                                throws SQLException {

                        DatabaseConnectionManager connectionManager = mock(DatabaseConnectionManager.class);

                        SQLException expectedException = new SQLException("Connection failed");

                        when(connectionManager.getConnection())
                                        .thenThrow(expectedException);

                        SQLiteUserRepository repository = new SQLiteUserRepository(connectionManager);

                        User user = User.reconstitute(
                                        USER_ID,
                                        UPDATED_FIRST_NAME,
                                        UPDATED_LAST_NAME);

                        PersistenceException actualException = assertThrows(
                                        PersistenceException.class,
                                        () -> repository.update(user));

                        assertSame(expectedException, actualException.getCause());
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

                        SQLiteUserRepository repository = new SQLiteUserRepository(connectionManager);

                        User user = User.reconstitute(
                                        USER_ID,
                                        UPDATED_FIRST_NAME,
                                        UPDATED_LAST_NAME);

                        PersistenceException actualException = assertThrows(
                                        PersistenceException.class,
                                        () -> repository.update(user));

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

                        SQLiteUserRepository repository = new SQLiteUserRepository(jdbc.connectionManager());

                        User user = User.reconstitute(
                                        USER_ID,
                                        UPDATED_FIRST_NAME,
                                        UPDATED_LAST_NAME);

                        PersistenceException actualException = assertThrows(
                                        PersistenceException.class,
                                        () -> repository.update(user));

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

                        SQLiteUserRepository repository = new SQLiteUserRepository(jdbc.connectionManager());

                        User user = User.reconstitute(
                                        USER_ID,
                                        UPDATED_FIRST_NAME,
                                        UPDATED_LAST_NAME);

                        PersistenceException actualException = assertThrows(
                                        PersistenceException.class,
                                        () -> repository.update(user));

                        assertSame(expectedException, actualException.getCause());

                        verify(jdbc.resultSet()).close();
                        verify(jdbc.statement()).close();
                        verify(jdbc.connection()).close();
                }

                @Test
                void throwsPersistenceExceptionWhenUserColumnsCannotBeRead()
                                throws SQLException {

                        JdbcMocks jdbc = createJdbcMocks();

                        SQLException expectedException = new SQLException("Unable to read updated user column");

                        when(jdbc.resultSet().next()).thenReturn(true);
                        when(jdbc.resultSet().getLong(USER_ID_COLUMN))
                                        .thenReturn(USER_ID);
                        when(jdbc.resultSet().getString(FIRST_NAME_COLUMN))
                                        .thenThrow(expectedException);

                        SQLiteUserRepository repository = new SQLiteUserRepository(jdbc.connectionManager());

                        User user = User.reconstitute(
                                        USER_ID,
                                        UPDATED_FIRST_NAME,
                                        UPDATED_LAST_NAME);

                        PersistenceException actualException = assertThrows(
                                        PersistenceException.class,
                                        () -> repository.update(user));

                        assertSame(expectedException, actualException.getCause());

                        verify(jdbc.resultSet()).close();
                        verify(jdbc.statement()).close();
                        verify(jdbc.connection()).close();
                }
        }

        @Nested
        @DisplayName("Tests for deleting users by ID, including validation, deletion results, and JDBC error handling")
        class DeleteByIdTests {

                @ParameterizedTest
                @ValueSource(longs = { 0L, -1L })
                void throwsIllegalArgumentExceptionWhenIdIsNotPositive(long id) {
                        DatabaseConnectionManager connectionManager = mock(DatabaseConnectionManager.class);

                        SQLiteUserRepository repository = new SQLiteUserRepository(connectionManager);

                        assertThrows(
                                        IllegalArgumentException.class,
                                        () -> repository.deleteById(id));

                        verifyNoInteractions(connectionManager);
                }

                @Test
                void successfullyDeletesUserAndReturnsTrue()
                                throws IOException {

                        DataSource dataSource = SQLiteDataSourceFactory.create(
                                        tempDir.resolve("delete-user.db"));

                        new DatabaseMigrationManager(dataSource).migrate();

                        DatabaseConnectionManager connectionManager = new DatabaseConnectionManager(dataSource);

                        SQLiteUserRepository repository = new SQLiteUserRepository(connectionManager);

                        User savedUser = repository.save(createNewUser());

                        boolean deleted = repository.deleteById(savedUser.getId());

                        assertTrue(deleted);

                        // Verify the user no longer exists.
                        assertTrue(repository.findById(savedUser.getId()).isEmpty());
                        assertTrue(repository.findAll().isEmpty());
                }

                @Test
                void successfullyReturnsFalseWhenNoUserDeleted()
                                throws IOException {

                        DataSource dataSource = SQLiteDataSourceFactory.create(
                                        tempDir.resolve("delete-missing-user.db"));

                        new DatabaseMigrationManager(dataSource).migrate();

                        DatabaseConnectionManager connectionManager = new DatabaseConnectionManager(dataSource);

                        SQLiteUserRepository repository = new SQLiteUserRepository(connectionManager);

                        // Insert a user to ensure an unsuccessful delete
                        // does not affect unrelated records.
                        User savedUser = repository.save(createNewUser());

                        boolean deleted = repository.deleteById(NONEXISTENT_USER_ID);

                        assertFalse(deleted);

                        // Verify the existing user remains unchanged.
                        Optional<User> retrievedUser = repository.findById(savedUser.getId());

                        assertTrue(retrievedUser.isPresent());
                        assertEquals(savedUser, retrievedUser.get());

                        assertEquals(1, repository.findAll().size());
                }

                @Test
                void throwsPersistenceExceptionWhenGetConnectionThrowsSQLException()
                                throws SQLException {

                        DatabaseConnectionManager connectionManager = mock(DatabaseConnectionManager.class);

                        SQLException expectedException = new SQLException("Connection failed");

                        when(connectionManager.getConnection())
                                        .thenThrow(expectedException);

                        SQLiteUserRepository repository = new SQLiteUserRepository(connectionManager);

                        PersistenceException actualException = assertThrows(
                                        PersistenceException.class,
                                        () -> repository.deleteById(USER_ID));

                        assertSame(expectedException, actualException.getCause());
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

                        SQLiteUserRepository repository = new SQLiteUserRepository(connectionManager);

                        PersistenceException actualException = assertThrows(
                                        PersistenceException.class,
                                        () -> repository.deleteById(USER_ID));

                        assertSame(expectedException, actualException.getCause());

                        verify(connection).close();
                }

                @Test
                void throwsPersistenceExceptionWhenExecuteUpdateThrowsSQLException()
                                throws SQLException {

                        DatabaseConnectionManager connectionManager = mock(DatabaseConnectionManager.class);

                        Connection connection = mock(Connection.class);
                        PreparedStatement statement = mock(PreparedStatement.class);

                        SQLException expectedException = new SQLException("Delete execution failed");

                        when(connectionManager.getConnection())
                                        .thenReturn(connection);

                        when(connection.prepareStatement(anyString()))
                                        .thenReturn(statement);

                        when(statement.executeUpdate())
                                        .thenThrow(expectedException);

                        SQLiteUserRepository repository = new SQLiteUserRepository(connectionManager);

                        PersistenceException actualException = assertThrows(
                                        PersistenceException.class,
                                        () -> repository.deleteById(USER_ID));

                        assertSame(expectedException, actualException.getCause());

                        verify(statement).close();
                        verify(connection).close();
                }
        }

        /**
         * Creates a valid, unpersisted user for repository tests.
         *
         * @return a user without a persistence ID
         */
        private static User createNewUser() {
                return User.builder()
                                .firstName(FIRST_NAME)
                                .lastName(LAST_NAME)
                                .build();
        }

        /**
         * Creates a mocked JDBC dependency chain for repository unit tests.
         *
         * @return the mocked connection manager, connection, statement,
         *         and result set
         * @throws SQLException if JDBC mock configuration fails
         */
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

        /**
         * Groups the mocked JDBC components used by repository tests.
         *
         * @param connectionManager the mocked database connection manager
         * @param connection        the mocked JDBC connection
         * @param statement         the mocked prepared statement
         * @param resultSet         the mocked result set
         */
        private record JdbcMocks(
                        DatabaseConnectionManager connectionManager,
                        Connection connection,
                        PreparedStatement statement,
                        ResultSet resultSet) {
        }
}
