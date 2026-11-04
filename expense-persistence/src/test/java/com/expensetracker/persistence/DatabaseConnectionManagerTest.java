
package com.expensetracker.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

/** Unit tests for {@link DatabaseConnectionManager} */
class DatabaseConnectionManagerTest {

    private static final String ENABLE_FOREIGN_KEYS = "PRAGMA foreign_keys = ON";

    @Test
    void throwsNullPointerExceptionWhenDataSourceIsNull() {
        assertThrows(NullPointerException.class,
                () -> new DatabaseConnectionManager(null));
    }

    @Test
    void successfullyReturnsConnectionFromDataSource()
            throws SQLException {

        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);

        DatabaseConnectionManager manager = new DatabaseConnectionManager(dataSource);

        Connection result = manager.getConnection();

        assertSame(connection, result);
        verify(dataSource).getConnection();
        verify(statement).close();
        verify(connection, org.mockito.Mockito.never()).close();
    }

    @Test
    void successfullyEnablesForeignKeysOnConnection()
            throws SQLException {

        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);

        DatabaseConnectionManager manager = new DatabaseConnectionManager(dataSource);

        manager.getConnection();

        verify(statement).execute(ENABLE_FOREIGN_KEYS);
        verify(statement).close();
    }

    @Test
    void throwsSQLExceptionWhenDataSourceCannotProvideConnection()
            throws SQLException {

        DataSource dataSource = mock(DataSource.class);
        SQLException expectedException = new SQLException("Connection failed");

        when(dataSource.getConnection()).thenThrow(expectedException);

        DatabaseConnectionManager manager = new DatabaseConnectionManager(dataSource);

        SQLException actualException = assertThrows(
                SQLException.class,
                manager::getConnection);

        assertSame(expectedException, actualException);
    }

    @Test
    void closesConnectionWhenStatementCreationFails()
            throws SQLException {

        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        SQLException expectedException = new SQLException("Statement creation failed");

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenThrow(expectedException);

        DatabaseConnectionManager manager = new DatabaseConnectionManager(dataSource);

        SQLException actualException = assertThrows(
                SQLException.class,
                manager::getConnection);

        assertSame(expectedException, actualException);
        verify(connection).close();
    }

    @Test
    void closesConnectionWhenForeignKeyInitializationFails()
            throws SQLException {

        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        SQLException expectedException = new SQLException("Foreign key initialization failed");

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);
        when(statement.execute(ENABLE_FOREIGN_KEYS))
                .thenThrow(expectedException);

        DatabaseConnectionManager manager = new DatabaseConnectionManager(dataSource);

        SQLException actualException = assertThrows(
                SQLException.class,
                manager::getConnection);

        assertSame(expectedException, actualException);
        verify(statement).close();
        verify(connection).close();
    }

    @Test
    void preservesOriginalSQLExceptionWhenConnectionCleanupFails()
            throws SQLException {

        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);

        SQLException initializationException = new SQLException("Initialization failed");
        SQLException cleanupException = new SQLException("Connection cleanup failed");

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement())
                .thenThrow(initializationException);
        org.mockito.Mockito.doThrow(cleanupException)
                .when(connection).close();

        DatabaseConnectionManager manager = new DatabaseConnectionManager(dataSource);

        SQLException actualException = assertThrows(
                SQLException.class,
                manager::getConnection);

        assertSame(initializationException, actualException);
        assertEquals(1, actualException.getSuppressed().length);
        assertSame(cleanupException, actualException.getSuppressed()[0]);
        verify(connection).close();
    }

    @Test
    void multipleCallsReturnNewConnections()
            throws SQLException {

        DataSource dataSource = mock(DataSource.class);

        Connection firstConnection = mock(Connection.class);
        Connection secondConnection = mock(Connection.class);

        Statement firstStatement = mock(Statement.class);
        Statement secondStatement = mock(Statement.class);

        when(dataSource.getConnection())
                .thenReturn(firstConnection, secondConnection);
        when(firstConnection.createStatement())
                .thenReturn(firstStatement);
        when(secondConnection.createStatement())
                .thenReturn(secondStatement);

        DatabaseConnectionManager manager = new DatabaseConnectionManager(dataSource);

        Connection firstResult = manager.getConnection();
        Connection secondResult = manager.getConnection();

        assertNotSame(firstResult, secondResult);
        assertSame(firstConnection, firstResult);
        assertSame(secondConnection, secondResult);

        verify(firstStatement).execute(ENABLE_FOREIGN_KEYS);
        verify(secondStatement).execute(ENABLE_FOREIGN_KEYS);
    }
}
