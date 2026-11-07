
package com.expensetracker.persistence;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import javax.sql.DataSource;

import org.sqlite.SQLiteDataSource;

/**
 * Factory for creating configured SQLite data sources.
 *
 * <p>
 * The factory supports both a default application database location
 * and a caller-specified database file path.
 * </p>
 *
 * <p>
 * Parent directories are created when necessary. The database file
 * itself is not explicitly created by this factory; SQLite creates
 * it when a connection is first established.
 * </p>
 */
public final class SQLiteDataSourceFactory {

    private static final String APPLICATION_DIRECTORY = "ExpenseTracker";
    private static final String DATABASE_FILE_NAME = "expenses.db";

    /**
     * Prevents instantiation of this utility class.
     */
    private SQLiteDataSourceFactory() {
    }

    /**
     * Creates a SQLite data source using the default database location.
     *
     * <p>
     * On Windows, the database is stored under the directory specified by
     * the {@code LOCALAPPDATA} environment variable when available.
     * Otherwise, the application falls back to the user's home directory
     * under {@code .local/share}.
     * </p>
     *
     * @return a configured SQLite data source
     * @throws IOException if the database directory cannot be created
     */
    public static DataSource create() throws IOException {
        return create(getDefaultDatabasePath());
    }

    /**
     * Creates a SQLite data source using the specified database file path.
     *
     * <p>
     * The path is converted to an absolute, normalized path. Its parent
     * directory is created if it does not already exist.
     * </p>
     *
     * @param databasePath the database file path; must not be null
     * @return a configured SQLite data source
     * @throws NullPointerException if databasePath is null
     * @throws IOException          if the parent directory cannot be created
     */
    public static DataSource create(Path databasePath) throws IOException {
        Path normalizedPath = Objects.requireNonNull(
                databasePath, "databasePath must not be null")
                .toAbsolutePath()
                .normalize();

        Files.createDirectories(normalizedPath.getParent());

        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl("jdbc:sqlite:" + normalizedPath);
        dataSource.setEnforceForeignKeys(true);

        return dataSource;
    }

    /**
     * Resolves the default database file path.
     *
     * @return the default path for the SQLite database file
     */
    private static Path getDefaultDatabasePath() {
        String localAppData = System.getenv("LOCALAPPDATA");

        Path applicationDataRoot = localAppData != null && !localAppData.isBlank()
                ? Path.of(localAppData)
                : Path.of(
                        System.getProperty("user.home"),
                        ".local",
                        "share");

        return applicationDataRoot
                .resolve(APPLICATION_DIRECTORY)
                .resolve(DATABASE_FILE_NAME);
    }
}
