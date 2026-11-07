
package com.expensetracker.persistence;

import java.util.Objects;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;

/**
 * Manages database schema migrations using Flyway.
 *
 * <p>
 * Migration scripts are loaded from the application's classpath
 * under {@code db/migration}. Flyway tracks applied migrations
 * in its schema history table and executes only pending migrations.
 * </p>
 *
 * <p>
 * This manager should be invoked during application startup,
 * before repositories begin accessing the database.
 * </p>
 */
public class DatabaseMigrationManager {

    private static final String MIGRATION_LOCATION = "classpath:db/migration";

    private final Flyway flyway;

    /**
     * Creates a migration manager using the supplied data source.
     *
     * @param dataSource the data source for the database being migrated;
     *                   must not be null
     * @throws NullPointerException if dataSource is null
     */
    public DatabaseMigrationManager(DataSource dataSource) {
        Objects.requireNonNull(dataSource, "dataSource must not be null");

        this.flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations(MIGRATION_LOCATION)
                .load();
    }

    /**
     * Applies all pending database migrations.
     *
     * <p>
     * Flyway validates previously applied migrations and executes
     * new migrations in version order. Successfully applied
     * migrations are not executed again on subsequent invocations.
     * </p>
     *
     * @throws org.flywaydb.core.api.FlywayException
     *                                               if migration validation or
     *                                               execution fails
     */
    public void migrate() {
        flyway.migrate();
    }
}
