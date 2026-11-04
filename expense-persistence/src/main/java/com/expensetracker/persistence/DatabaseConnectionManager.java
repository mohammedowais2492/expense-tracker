
package com.expensetracker.persistence;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;

import javax.sql.DataSource;

/**
 * Manages the creation and initialization of JDBC database connections.
 *
 * <p>
 * Connections are obtained from an injected {@link DataSource}.
 * Each connection is configured to enforce SQLite foreign-key constraints
 * before being returned to the caller.
 * </p>
 *
 * <p>
 * Callers are responsible for closing the returned connections,
 * preferably using try-with-resources.
 * </p>
 */
public class DatabaseConnectionManager {

    private final DataSource dataSource;

    /**
     * Creates a connection manager backed by the specified data source.
     *
     * @param dataSource the data source used to obtain database connections;
     *                   must not be null
     * @throws NullPointerException if dataSource is null
     */
    public DatabaseConnectionManager(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(
                dataSource, "dataSource must not be null");
    }

    /**
     * Obtains and initializes a new JDBC connection.
     *
     * <p>
     * Foreign-key enforcement is enabled on the connection using SQLite's
     * {@code PRAGMA foreign_keys = ON} statement.
     * </p>
     *
     * <p>
     * If initialization fails, the connection is closed before the original
     * exception is propagated. Any exception encountered during cleanup
     * is attached to the original exception as a suppressed exception.
     * </p>
     *
     * @return an initialized JDBC connection
     * @throws SQLException if a connection cannot be obtained or initialized
     */
    public Connection getConnection() throws SQLException {
        Connection connection = dataSource.getConnection();

        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
            return connection;
        } catch (SQLException exception) {
            try {
                connection.close();
            } catch (SQLException closeException) {
                exception.addSuppressed(closeException);
            }
            throw exception;
        }
    }
}
