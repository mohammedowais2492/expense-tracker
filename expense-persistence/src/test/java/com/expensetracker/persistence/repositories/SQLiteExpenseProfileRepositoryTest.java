
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
import java.util.Optional;

import javax.sql.DataSource;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.expensetracker.models.ExpenseProfile;
import com.expensetracker.models.User;
import com.expensetracker.persistence.DatabaseConnectionManager;
import com.expensetracker.persistence.DatabaseMigrationManager;
import com.expensetracker.persistence.SQLiteDataSourceFactory;
import com.expensetracker.persistence.exceptions.PersistenceException;

/**
 * Tests for {@link SQLiteExpenseProfileRepository}.
 */
class SQLiteExpenseProfileRepositoryTest {

    private static final long PROFILE_ID = 1L;
    private static final long NONEXISTENT_USER_ID = 9999L;
    private static final long NONEXISTENT_PROFILE_ID = 9999L;

    private static final String FIRST_NAME = "Sample";
    private static final String LAST_NAME = "Person";
    private static final String PROFILE_NAME = "Personal";
    private static final String UPDATED_PROFILE_NAME = "Updated Personal";

    private static final String PROFILE_ID_COLUMN = "ProfileId";
    private static final String USER_ID_COLUMN = "UserId";
    private static final String PROFILE_NAME_COLUMN = "ProfileName";

    private static final String SELECT_PROFILE_BY_ID_SQL = String.format("""
            SELECT %s, %s, %s
            FROM ExpenseProfile
            WHERE %s = ?
            """,
            PROFILE_ID_COLUMN,
            USER_ID_COLUMN,
            PROFILE_NAME_COLUMN,
            PROFILE_ID_COLUMN);

    @TempDir
    Path tempDir;

    @Nested
    @DisplayName("Tests for SQLiteExpenseProfileRepository constructor validation")
    class ConstructorTests {

        @Test
        void throwsNullPointerExceptionWhenConnectionManagerIsNull() {
            assertThrows(
                    NullPointerException.class,
                    () -> new SQLiteExpenseProfileRepository(null));
        }
    }

    @Nested
    @DisplayName("Tests for saving expense profiles, including validation, persistence, and JDBC error handling")
    class SaveTests {

        @Test
        void throwsNullPointerExceptionWhenProfileIsNull() {
            DatabaseConnectionManager connectionManager = mock(DatabaseConnectionManager.class);

            SQLiteExpenseProfileRepository repository = new SQLiteExpenseProfileRepository(connectionManager);

            assertThrows(
                    NullPointerException.class,
                    () -> repository.save(null));

            verifyNoInteractions(connectionManager);
        }

        @Test
        void throwsIllegalArgumentExceptionWhenProfileIdIsNotNull() {
            DatabaseConnectionManager connectionManager = mock(DatabaseConnectionManager.class);

            SQLiteExpenseProfileRepository repository = new SQLiteExpenseProfileRepository(connectionManager);

            ExpenseProfile persistedProfile = ExpenseProfile.reconstitute(
                    PROFILE_ID,
                    1L,
                    PROFILE_NAME);

            assertThrows(
                    IllegalArgumentException.class,
                    () -> repository.save(persistedProfile));

            verifyNoInteractions(connectionManager);
        }

        @Test
        void successfullyInsertsANewProfileAndReturnsReconstitutedProfile()
                throws IOException, SQLException {

            DataSource dataSource = SQLiteDataSourceFactory.create(
                    tempDir.resolve("save-profile.db"));

            new DatabaseMigrationManager(dataSource).migrate();

            DatabaseConnectionManager connectionManager = new DatabaseConnectionManager(dataSource);

            SQLiteUserRepository userRepository = new SQLiteUserRepository(connectionManager);

            SQLiteExpenseProfileRepository repository = new SQLiteExpenseProfileRepository(connectionManager);

            User savedUser = userRepository.save(createNewUser());

            ExpenseProfile profile = createNewProfile(savedUser.getId());

            ExpenseProfile savedProfile = repository.save(profile);

            // Verify the returned domain object.
            assertNotNull(savedProfile.getId());
            assertTrue(savedProfile.getId() > 0);
            assertEquals(savedUser.getId().longValue(),
                    savedProfile.getUserId());
            assertEquals(PROFILE_NAME, savedProfile.getProfileName());

            // Verify the original immutable profile is unchanged.
            assertNull(profile.getId());

            // Verify the record was actually persisted in SQLite.
            try (Connection connection = dataSource.getConnection();
                    PreparedStatement statement = connection.prepareStatement(
                            SELECT_PROFILE_BY_ID_SQL)) {

                statement.setLong(1, savedProfile.getId());

                try (ResultSet resultSet = statement.executeQuery()) {
                    assertTrue(resultSet.next());

                    assertEquals(
                            savedProfile.getId().longValue(),
                            resultSet.getLong(PROFILE_ID_COLUMN));

                    assertEquals(
                            savedUser.getId().longValue(),
                            resultSet.getLong(USER_ID_COLUMN));

                    assertEquals(
                            PROFILE_NAME,
                            resultSet.getString(PROFILE_NAME_COLUMN));

                    assertFalse(resultSet.next());
                }
            }
        }

        @Test
        void throwsPersistenceExceptionWhenUserDoesNotExist()
                throws IOException {

            DataSource dataSource = SQLiteDataSourceFactory.create(
                    tempDir.resolve("save-profile-missing-user.db"));

            new DatabaseMigrationManager(dataSource).migrate();

            DatabaseConnectionManager connectionManager = new DatabaseConnectionManager(dataSource);

            SQLiteExpenseProfileRepository repository = new SQLiteExpenseProfileRepository(connectionManager);

            ExpenseProfile profile = createNewProfile(NONEXISTENT_USER_ID);

            PersistenceException actualException = assertThrows(
                    PersistenceException.class,
                    () -> repository.save(profile));

            assertNotNull(actualException.getCause());
            assertTrue(actualException.getCause() instanceof SQLException);
        }

        @Test
        void throwsPersistenceExceptionWhenGetConnectionThrowsSQLException()
                throws SQLException {

            DatabaseConnectionManager connectionManager = mock(DatabaseConnectionManager.class);

            SQLException expectedException = new SQLException("Connection failed");

            when(connectionManager.getConnection())
                    .thenThrow(expectedException);

            SQLiteExpenseProfileRepository repository = new SQLiteExpenseProfileRepository(connectionManager);

            PersistenceException actualException = assertThrows(
                    PersistenceException.class,
                    () -> repository.save(createNewProfile(1L)));

            assertSame(expectedException, actualException.getCause());
            assertEquals("Profile could not be created",
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

            SQLiteExpenseProfileRepository repository = new SQLiteExpenseProfileRepository(connectionManager);

            PersistenceException actualException = assertThrows(
                    PersistenceException.class,
                    () -> repository.save(createNewProfile(1L)));

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

            SQLiteExpenseProfileRepository repository = new SQLiteExpenseProfileRepository(
                    jdbc.connectionManager());

            PersistenceException actualException = assertThrows(
                    PersistenceException.class,
                    () -> repository.save(createNewProfile(1L)));

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

            SQLiteExpenseProfileRepository repository = new SQLiteExpenseProfileRepository(
                    jdbc.connectionManager());

            PersistenceException actualException = assertThrows(
                    PersistenceException.class,
                    () -> repository.save(createNewProfile(1L)));

            assertSame(expectedException, actualException.getCause());

            verify(jdbc.resultSet()).close();
            verify(jdbc.statement()).close();
            verify(jdbc.connection()).close();
        }

        @Test
        void throwsPersistenceExceptionWhenNoProfileIdIsGenerated()
                throws SQLException {

            JdbcMocks jdbc = createJdbcMocks();

            when(jdbc.resultSet().next()).thenReturn(false);

            SQLiteExpenseProfileRepository repository = new SQLiteExpenseProfileRepository(
                    jdbc.connectionManager());

            PersistenceException actualException = assertThrows(
                    PersistenceException.class,
                    () -> repository.save(createNewProfile(1L)));

            assertNotNull(actualException.getCause());
            assertTrue(actualException.getCause() instanceof SQLException);

            assertEquals(
                    "Insert did not generate a profileId",
                    actualException.getCause().getMessage());

            verify(jdbc.resultSet()).close();
            verify(jdbc.statement()).close();
            verify(jdbc.connection()).close();
        }

        @Test
        void throwsPersistenceExceptionWhenProfileColumnCannotBeRead()
                throws SQLException {

            JdbcMocks jdbc = createJdbcMocks();

            SQLException expectedException = new SQLException("Unable to read generated profile ID");

            when(jdbc.resultSet().next()).thenReturn(true);

            when(jdbc.resultSet().getLong(PROFILE_ID_COLUMN))
                    .thenThrow(expectedException);

            SQLiteExpenseProfileRepository repository = new SQLiteExpenseProfileRepository(
                    jdbc.connectionManager());

            PersistenceException actualException = assertThrows(
                    PersistenceException.class,
                    () -> repository.save(createNewProfile(1L)));

            assertSame(expectedException, actualException.getCause());

            verify(jdbc.resultSet()).close();
            verify(jdbc.statement()).close();
            verify(jdbc.connection()).close();
        }
    }

    @Nested
    @DisplayName("Tests for finding expense profiles by user ID, including validation, retrieval, and JDBC error handling")
    class FindByUserIdTests {

        @ParameterizedTest
        @ValueSource(longs = { 0L, -1L })
        void throwsIllegalArgumentExceptionWhenUserIdIsNotPositive(long userId) {
            DatabaseConnectionManager connectionManager = mock(DatabaseConnectionManager.class);

            SQLiteExpenseProfileRepository repository = new SQLiteExpenseProfileRepository(connectionManager);

            assertThrows(
                    IllegalArgumentException.class,
                    () -> repository.findByUserId(userId));

            verifyNoInteractions(connectionManager);
        }

        @Test
        void successfullyReturnsProfileWhenMatchIsFound()
                throws IOException {

            DataSource dataSource = SQLiteDataSourceFactory.create(
                    tempDir.resolve("find-profile-by-user-id.db"));

            new DatabaseMigrationManager(dataSource).migrate();

            DatabaseConnectionManager connectionManager = new DatabaseConnectionManager(dataSource);

            SQLiteUserRepository userRepository = new SQLiteUserRepository(connectionManager);

            SQLiteExpenseProfileRepository repository = new SQLiteExpenseProfileRepository(connectionManager);

            User savedUser = userRepository.save(createNewUser());

            ExpenseProfile savedProfile = repository.save(
                    createNewProfile(savedUser.getId()));

            Optional<ExpenseProfile> result = repository.findByUserId(savedUser.getId());

            assertTrue(result.isPresent());
            assertEquals(savedProfile, result.get());

            assertEquals(
                    savedProfile.getId(),
                    result.get().getId());

            assertEquals(
                    savedUser.getId().longValue(),
                    result.get().getUserId());

            assertEquals(
                    PROFILE_NAME,
                    result.get().getProfileName());
        }

        @Test
        void successfullyReturnsEmptyWhenNoProfileMatchesUserId()
                throws IOException {

            DataSource dataSource = SQLiteDataSourceFactory.create(
                    tempDir.resolve("find-missing-profile.db"));

            new DatabaseMigrationManager(dataSource).migrate();

            DatabaseConnectionManager connectionManager = new DatabaseConnectionManager(dataSource);

            SQLiteUserRepository userRepository = new SQLiteUserRepository(connectionManager);

            SQLiteExpenseProfileRepository repository = new SQLiteExpenseProfileRepository(connectionManager);

            // The user exists but does not have an expense profile.
            User savedUser = userRepository.save(createNewUser());

            Optional<ExpenseProfile> result = repository.findByUserId(savedUser.getId());

            assertTrue(result.isEmpty());
        }

        @Test
        void throwsPersistenceExceptionWhenGetConnectionThrowsSQLException()
                throws SQLException {

            DatabaseConnectionManager connectionManager = mock(DatabaseConnectionManager.class);

            SQLException expectedException = new SQLException("Connection failed");

            when(connectionManager.getConnection())
                    .thenThrow(expectedException);

            SQLiteExpenseProfileRepository repository = new SQLiteExpenseProfileRepository(connectionManager);

            PersistenceException actualException = assertThrows(
                    PersistenceException.class,
                    () -> repository.findByUserId(NONEXISTENT_USER_ID));

            assertSame(expectedException, actualException.getCause());

            assertEquals(
                    "Unable to find profile by userId",
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

            SQLiteExpenseProfileRepository repository = new SQLiteExpenseProfileRepository(connectionManager);

            PersistenceException actualException = assertThrows(
                    PersistenceException.class,
                    () -> repository.findByUserId(NONEXISTENT_USER_ID));

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

            SQLiteExpenseProfileRepository repository = new SQLiteExpenseProfileRepository(
                    jdbc.connectionManager());

            PersistenceException actualException = assertThrows(
                    PersistenceException.class,
                    () -> repository.findByUserId(NONEXISTENT_USER_ID));

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

            SQLiteExpenseProfileRepository repository = new SQLiteExpenseProfileRepository(
                    jdbc.connectionManager());

            PersistenceException actualException = assertThrows(
                    PersistenceException.class,
                    () -> repository.findByUserId(NONEXISTENT_USER_ID));

            assertSame(expectedException, actualException.getCause());

            verify(jdbc.resultSet()).close();
            verify(jdbc.statement()).close();
            verify(jdbc.connection()).close();
        }

        @Test
        void throwsPersistenceExceptionWhenProfileColumnsCannotBeRead()
                throws SQLException {

            JdbcMocks jdbc = createJdbcMocks();

            SQLException expectedException = new SQLException("Unable to read profile column");

            when(jdbc.resultSet().next()).thenReturn(true);

            when(jdbc.resultSet().getLong(PROFILE_ID_COLUMN))
                    .thenReturn(PROFILE_ID);

            when(jdbc.resultSet().getLong(USER_ID_COLUMN))
                    .thenThrow(expectedException);

            SQLiteExpenseProfileRepository repository = new SQLiteExpenseProfileRepository(
                    jdbc.connectionManager());

            PersistenceException actualException = assertThrows(
                    PersistenceException.class,
                    () -> repository.findByUserId(NONEXISTENT_USER_ID));

            assertSame(expectedException, actualException.getCause());

            verify(jdbc.resultSet()).close();
            verify(jdbc.statement()).close();
            verify(jdbc.connection()).close();
        }
    }

    @Nested
    @DisplayName("Tests for updating expense profiles, including validation, persistence, and JDBC error handling")
    class UpdateTests {

        @Test
        void throwsNullPointerExceptionWhenProfileIsNull() {
            DatabaseConnectionManager connectionManager = mock(DatabaseConnectionManager.class);

            SQLiteExpenseProfileRepository repository = new SQLiteExpenseProfileRepository(connectionManager);

            assertThrows(
                    NullPointerException.class,
                    () -> repository.update(null));

            verifyNoInteractions(connectionManager);
        }

        @Test
        void throwsIllegalArgumentExceptionWhenProfileIdIsNull() {
            DatabaseConnectionManager connectionManager = mock(DatabaseConnectionManager.class);

            SQLiteExpenseProfileRepository repository = new SQLiteExpenseProfileRepository(connectionManager);

            ExpenseProfile profile = createNewProfile(1L);

            assertThrows(
                    IllegalArgumentException.class,
                    () -> repository.update(profile));

            verifyNoInteractions(connectionManager);
        }

        @Test
        void successfullyUpdatesProfile() throws IOException {
            DataSource dataSource = SQLiteDataSourceFactory.create(
                    tempDir.resolve("update-profile.db"));

            new DatabaseMigrationManager(dataSource).migrate();

            DatabaseConnectionManager connectionManager = new DatabaseConnectionManager(dataSource);

            SQLiteUserRepository userRepository = new SQLiteUserRepository(connectionManager);

            SQLiteExpenseProfileRepository repository = new SQLiteExpenseProfileRepository(connectionManager);

            User savedUser = userRepository.save(createNewUser());

            ExpenseProfile originalProfile = repository.save(
                    createNewProfile(savedUser.getId()));

            ExpenseProfile profileToUpdate = ExpenseProfile.reconstitute(
                    originalProfile.getId(),
                    originalProfile.getUserId(),
                    UPDATED_PROFILE_NAME);

            ExpenseProfile updatedProfile = repository.update(profileToUpdate);

            // Verify the returned profile.
            assertEquals(originalProfile.getId(), updatedProfile.getId());
            assertEquals(originalProfile.getUserId(), updatedProfile.getUserId());
            assertEquals(UPDATED_PROFILE_NAME, updatedProfile.getProfileName());

            // Verify the updated values were persisted.
            Optional<ExpenseProfile> retrievedProfile = repository.findByUserId(savedUser.getId());

            assertTrue(retrievedProfile.isPresent());
            assertEquals(updatedProfile, retrievedProfile.get());

            // Verify the original immutable profile remains unchanged.
            assertEquals(PROFILE_NAME, originalProfile.getProfileName());

            // Verify that the owning user was not changed.
            assertEquals(savedUser.getId().longValue(),
                    retrievedProfile.get().getUserId());
        }

        @Test
        void throwsIllegalArgumentExceptionWhenNoProfileUpdated()
                throws IOException {

            DataSource dataSource = SQLiteDataSourceFactory.create(
                    tempDir.resolve("update-missing-profile.db"));

            new DatabaseMigrationManager(dataSource).migrate();

            DatabaseConnectionManager connectionManager = new DatabaseConnectionManager(dataSource);

            SQLiteUserRepository userRepository = new SQLiteUserRepository(connectionManager);

            SQLiteExpenseProfileRepository repository = new SQLiteExpenseProfileRepository(connectionManager);

            User savedUser = userRepository.save(createNewUser());

            // The user exists, but the profile does not.
            ExpenseProfile nonexistentProfile = ExpenseProfile.reconstitute(
                    PROFILE_ID,
                    savedUser.getId(),
                    UPDATED_PROFILE_NAME);

            IllegalArgumentException actualException = assertThrows(
                    IllegalArgumentException.class,
                    () -> repository.update(nonexistentProfile));

            assertEquals(
                    "profile does not exist with id: " + PROFILE_ID,
                    actualException.getMessage());

            assertTrue(repository.findByUserId(savedUser.getId()).isEmpty());
        }

        @Test
        void throwsPersistenceExceptionWhenGetConnectionThrowsSQLException()
                throws SQLException {

            DatabaseConnectionManager connectionManager = mock(DatabaseConnectionManager.class);

            SQLException expectedException = new SQLException("Connection failed");

            when(connectionManager.getConnection())
                    .thenThrow(expectedException);

            SQLiteExpenseProfileRepository repository = new SQLiteExpenseProfileRepository(connectionManager);

            ExpenseProfile profile = ExpenseProfile.reconstitute(
                    PROFILE_ID, 1L, UPDATED_PROFILE_NAME);

            PersistenceException actualException = assertThrows(
                    PersistenceException.class,
                    () -> repository.update(profile));

            assertSame(expectedException, actualException.getCause());
            assertEquals("Unable to update profile",
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

            SQLiteExpenseProfileRepository repository = new SQLiteExpenseProfileRepository(connectionManager);

            ExpenseProfile profile = ExpenseProfile.reconstitute(
                    PROFILE_ID, 1L, UPDATED_PROFILE_NAME);

            PersistenceException actualException = assertThrows(
                    PersistenceException.class,
                    () -> repository.update(profile));

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

            SQLiteExpenseProfileRepository repository = new SQLiteExpenseProfileRepository(
                    jdbc.connectionManager());

            ExpenseProfile profile = ExpenseProfile.reconstitute(
                    PROFILE_ID, 1L, UPDATED_PROFILE_NAME);

            PersistenceException actualException = assertThrows(
                    PersistenceException.class,
                    () -> repository.update(profile));

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

            SQLiteExpenseProfileRepository repository = new SQLiteExpenseProfileRepository(
                    jdbc.connectionManager());

            ExpenseProfile profile = ExpenseProfile.reconstitute(
                    PROFILE_ID, 1L, UPDATED_PROFILE_NAME);

            PersistenceException actualException = assertThrows(
                    PersistenceException.class,
                    () -> repository.update(profile));

            assertSame(expectedException, actualException.getCause());

            verify(jdbc.resultSet()).close();
            verify(jdbc.statement()).close();
            verify(jdbc.connection()).close();
        }

        @Test
        void throwsPersistenceExceptionWhenProfileColumnsCannotBeRead()
                throws SQLException {

            JdbcMocks jdbc = createJdbcMocks();

            SQLException expectedException = new SQLException("Unable to read updated profile column");

            when(jdbc.resultSet().next()).thenReturn(true);

            when(jdbc.resultSet().getLong(PROFILE_ID_COLUMN))
                    .thenReturn(PROFILE_ID);

            when(jdbc.resultSet().getLong(USER_ID_COLUMN))
                    .thenReturn(1L);

            when(jdbc.resultSet().getString(PROFILE_NAME_COLUMN))
                    .thenThrow(expectedException);

            SQLiteExpenseProfileRepository repository = new SQLiteExpenseProfileRepository(
                    jdbc.connectionManager());

            ExpenseProfile profile = ExpenseProfile.reconstitute(
                    PROFILE_ID, 1L, UPDATED_PROFILE_NAME);

            PersistenceException actualException = assertThrows(
                    PersistenceException.class,
                    () -> repository.update(profile));

            assertSame(expectedException, actualException.getCause());

            verify(jdbc.resultSet()).close();
            verify(jdbc.statement()).close();
            verify(jdbc.connection()).close();
        }
    }

    @Nested
    @DisplayName("Tests for deleting expense profiles by ID, including validation, deletion results, and JDBC error handling")
    class DeleteByIdTests {

        @ParameterizedTest
        @ValueSource(longs = { 0L, -1L })
        void throwsIllegalArgumentExceptionWhenIdIsNotPositive(long id) {
            DatabaseConnectionManager connectionManager = mock(DatabaseConnectionManager.class);

            SQLiteExpenseProfileRepository repository = new SQLiteExpenseProfileRepository(connectionManager);

            assertThrows(
                    IllegalArgumentException.class,
                    () -> repository.deleteById(id));

            verifyNoInteractions(connectionManager);
        }

        @Test
        void successfullyDeletesProfileAndReturnsTrue()
                throws IOException {

            DataSource dataSource = SQLiteDataSourceFactory.create(
                    tempDir.resolve("delete-profile.db"));

            new DatabaseMigrationManager(dataSource).migrate();

            DatabaseConnectionManager connectionManager = new DatabaseConnectionManager(dataSource);

            SQLiteUserRepository userRepository = new SQLiteUserRepository(connectionManager);

            SQLiteExpenseProfileRepository repository = new SQLiteExpenseProfileRepository(connectionManager);

            User savedUser = userRepository.save(createNewUser());

            ExpenseProfile savedProfile = repository.save(
                    createNewProfile(savedUser.getId()));

            boolean deleted = repository.deleteById(savedProfile.getId());

            assertTrue(deleted);

            // Verify the profile no longer exists.
            assertTrue(repository.findByUserId(savedUser.getId()).isEmpty());

            // Verify the owning user was not deleted.
            Optional<User> retrievedUser = userRepository.findById(savedUser.getId());

            assertTrue(retrievedUser.isPresent());
            assertEquals(savedUser, retrievedUser.get());
        }

        @Test
        void successfullyReturnsFalseWhenNoProfileDeleted()
                throws IOException {

            DataSource dataSource = SQLiteDataSourceFactory.create(
                    tempDir.resolve("delete-missing-profile.db"));

            new DatabaseMigrationManager(dataSource).migrate();

            DatabaseConnectionManager connectionManager = new DatabaseConnectionManager(dataSource);

            SQLiteUserRepository userRepository = new SQLiteUserRepository(connectionManager);

            SQLiteExpenseProfileRepository repository = new SQLiteExpenseProfileRepository(connectionManager);

            User savedUser = userRepository.save(createNewUser());

            ExpenseProfile savedProfile = repository.save(
                    createNewProfile(savedUser.getId()));

            boolean deleted = repository.deleteById(
                    NONEXISTENT_PROFILE_ID);

            assertFalse(deleted);

            // Verify the existing profile remains unchanged.
            Optional<ExpenseProfile> retrievedProfile = repository.findByUserId(savedUser.getId());

            assertTrue(retrievedProfile.isPresent());
            assertEquals(savedProfile, retrievedProfile.get());
        }

        @Test
        void throwsPersistenceExceptionWhenGetConnectionThrowsSQLException()
                throws SQLException {

            DatabaseConnectionManager connectionManager = mock(DatabaseConnectionManager.class);

            SQLException expectedException = new SQLException("Connection failed");

            when(connectionManager.getConnection())
                    .thenThrow(expectedException);

            SQLiteExpenseProfileRepository repository = new SQLiteExpenseProfileRepository(connectionManager);

            PersistenceException actualException = assertThrows(
                    PersistenceException.class,
                    () -> repository.deleteById(PROFILE_ID));

            assertSame(expectedException, actualException.getCause());

            assertEquals(
                    "Unable to delete profile",
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

            SQLiteExpenseProfileRepository repository = new SQLiteExpenseProfileRepository(connectionManager);

            PersistenceException actualException = assertThrows(
                    PersistenceException.class,
                    () -> repository.deleteById(PROFILE_ID));

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

            SQLiteExpenseProfileRepository repository = new SQLiteExpenseProfileRepository(connectionManager);

            PersistenceException actualException = assertThrows(
                    PersistenceException.class,
                    () -> repository.deleteById(PROFILE_ID));

            assertSame(expectedException, actualException.getCause());

            verify(statement).close();
            verify(connection).close();
        }
    }

    private static User createNewUser() {
        return User.builder()
                .firstName(FIRST_NAME)
                .lastName(LAST_NAME)
                .build();
    }

    private static ExpenseProfile createNewProfile(long userId) {
        return ExpenseProfile.builder()
                .userId(userId)
                .profileName(PROFILE_NAME)
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
