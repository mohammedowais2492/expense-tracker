package com.expensetracker.persistence.repositories;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;

import com.expensetracker.models.ExpenseProfile;
import com.expensetracker.persistence.DatabaseConnectionManager;
import com.expensetracker.persistence.exceptions.PersistenceException;
import com.expensetracker.repositories.ExpenseProfileRepository;

/**
 * SQLite-backed implementation of {@link ExpenseProfileRepository}.
 *
 * <p>
 * Uses JDBC to persist and retrieve {@link ExpenseProfile} instances.
 * Database connections are obtained from the injected
 * {@link DatabaseConnectionManager}.
 * </p>
 */
public class SQLiteExpenseProfileRepository implements ExpenseProfileRepository {
    private static final String PROFILE_ID_COLUMN = "ProfileId";
    private static final String USER_ID_COLUMN = "UserId";
    private static final String PROFILE_NAME_COLUMN = "ProfileName";
    private static final String INSERT_PROFILE_SQL = String.format("""
                    INSERT INTO ExpenseProfile (%s, %s)
                    VALUES (?, ?)
                    RETURNING %s
            """, USER_ID_COLUMN, PROFILE_NAME_COLUMN, PROFILE_ID_COLUMN);
    private static final String FIND_BY_USER_ID_SQL = String.format("""
                    SELECT %s, %s, %s
                    FROM ExpenseProfile
                    WHERE %s = ?
            """, PROFILE_ID_COLUMN, USER_ID_COLUMN, PROFILE_NAME_COLUMN, USER_ID_COLUMN);
    private static final String UPDATE_SQL = String.format("""
                    UPDATE ExpenseProfile
                    SET %s = ?
                    WHERE %s = ?
                    RETURNING %s, %s, %s
            """, PROFILE_NAME_COLUMN, PROFILE_ID_COLUMN, PROFILE_ID_COLUMN, USER_ID_COLUMN, PROFILE_NAME_COLUMN);
    private static final String DELETE_SQL = String.format("""
                    DELETE FROM ExpenseProfile
                    WHERE %s = ?
            """, PROFILE_ID_COLUMN);

    private final DatabaseConnectionManager databaseConnectionManager;

    /**
     * Creates a SQLite-backed expense profile repository using the
     * specified database connection manager.
     *
     * @param databaseConnectionManager the manager used to obtain
     *                                  database connections; must not be null
     * @throws NullPointerException if databaseConnectionManager is null
     */
    public SQLiteExpenseProfileRepository(DatabaseConnectionManager databaseConnectionManager) {
        this.databaseConnectionManager = Objects.requireNonNull(databaseConnectionManager,
                "databaseConnectionManager must not be null");
    }

    @Override
    public ExpenseProfile save(ExpenseProfile profile) {
        Objects.requireNonNull(profile, "profile must not be null");
        if (profile.getId() != null) {
            throw new IllegalArgumentException("profileId must be null");
        }
        try (Connection connection = databaseConnectionManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(INSERT_PROFILE_SQL)) {
            statement.setLong(1, profile.getUserId());
            statement.setString(2, profile.getProfileName());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new SQLException("Insert did not generate a profileId");
                }
                long profileId = resultSet.getLong(PROFILE_ID_COLUMN);
                return ExpenseProfile.reconstitute(profileId, profile.getUserId(), profile.getProfileName());
            }
        } catch (SQLException exception) {
            throw new PersistenceException("Profile could not be created", exception);
        }
    }

    @Override
    public Optional<ExpenseProfile> findByUserId(long userId) {
        if (userId <= 0) {
            throw new IllegalArgumentException("userId must be positive");
        }
        try (Connection connection = databaseConnectionManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(FIND_BY_USER_ID_SQL)) {
            statement.setLong(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapExpenseProfile(resultSet));
            }
        } catch (SQLException exception) {
            throw new PersistenceException("Unable to find profile by userId", exception);
        }
    }

    @Override
    public ExpenseProfile update(ExpenseProfile profile) {
        Objects.requireNonNull(profile, "profile must not be null");
        if (profile.getId() == null) {
            throw new IllegalArgumentException("id cannot be null");
        }
        try (Connection connection = databaseConnectionManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(UPDATE_SQL)) {
            statement.setString(1, profile.getProfileName());
            statement.setLong(2, profile.getId());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new IllegalArgumentException("profile does not exist with id: " + profile.getId());
                }
                return mapExpenseProfile(resultSet);
            }
        } catch (SQLException exception) {
            throw new PersistenceException("Unable to update profile", exception);
        }
    }

    @Override
    public boolean deleteById(long id) {
        if (id <= 0) {
            throw new IllegalArgumentException("id must be positive");
        }
        try (Connection connection = databaseConnectionManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(DELETE_SQL)) {
            statement.setLong(1, id);
            return statement.executeUpdate() == 1;
        } catch (SQLException exception) {
            throw new PersistenceException("Unable to delete profile", exception);
        }
    }

    private static ExpenseProfile mapExpenseProfile(ResultSet resultSet) throws SQLException {
        long profileId = resultSet.getLong(PROFILE_ID_COLUMN);
        long userId = resultSet.getLong(USER_ID_COLUMN);
        String profileName = resultSet.getString(PROFILE_NAME_COLUMN);
        return ExpenseProfile.reconstitute(profileId, userId, profileName);
    }
}
