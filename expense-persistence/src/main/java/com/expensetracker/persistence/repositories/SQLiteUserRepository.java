package com.expensetracker.persistence.repositories;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.expensetracker.models.User;
import com.expensetracker.persistence.DatabaseConnectionManager;
import com.expensetracker.persistence.exceptions.PersistenceException;
import com.expensetracker.repositories.UserRepository;

/**
 * SQLite-backed implementation of {@link UserRepository}.
 *
 * <p>
 * Uses JDBC to persist and retrieve {@link User} instances.
 * Database connections are obtained from the injected
 * {@link DatabaseConnectionManager}.
 * </p>
 */
public class SQLiteUserRepository implements UserRepository {
    private static final String USER_ID_COLUMN = "UserId";
    private static final String FIRST_NAME_COLUMN = "FirstName";
    private static final String LAST_NAME_COLUMN = "LastName";
    private static final String INSERT_USER_SQL = String.format("""
                    INSERT INTO User (%s, %s)
                    VALUES (?, ?)
                    RETURNING %s
            """, FIRST_NAME_COLUMN, LAST_NAME_COLUMN, USER_ID_COLUMN);
    private static final String FIND_USER_BY_ID_SQL = String.format("""
                    SELECT %s, %s, %s
                    FROM USER
                    WHERE %s = ?
            """, USER_ID_COLUMN, FIRST_NAME_COLUMN, LAST_NAME_COLUMN, USER_ID_COLUMN);
    private static final String FIND_ALL_SQL = String.format("""
                    SELECT %s, %s, %s
                    FROM USER
                    ORDER BY %s
            """, USER_ID_COLUMN, FIRST_NAME_COLUMN, LAST_NAME_COLUMN, USER_ID_COLUMN);
    private static final String UPDATE_SQL = String.format("""
                    UPDATE User
                    SET %s = ?, %s = ?
                    WHERE %s = ?
                    RETURNING %s, %s, %s
            """, FIRST_NAME_COLUMN, LAST_NAME_COLUMN, USER_ID_COLUMN, USER_ID_COLUMN, FIRST_NAME_COLUMN,
            LAST_NAME_COLUMN);
    private static final String DELETE_SQL = String.format("""
                    DELETE FROM USER
                    WHERE %s = ?
            """, USER_ID_COLUMN);
    private final DatabaseConnectionManager databaseConnectionManager;

    /**
     * Creates a SQLite-backed user repository using the specified
     * database connection manager.
     *
     * <p>
     * The connection manager is used to obtain JDBC connections
     * for persistence operations.
     * </p>
     *
     * @param databaseConnectionManager the connection manager used to obtain
     *                                  database connections; must not be null
     * @throws NullPointerException if databaseConnectionManager is null
     */
    public SQLiteUserRepository(DatabaseConnectionManager databaseConnectionManager) {
        this.databaseConnectionManager = Objects.requireNonNull(databaseConnectionManager,
                "databaseConnectionManager must not be null");
    }

    @Override
    public User save(User user) {
        Objects.requireNonNull(user, "user must not be null");
        if (user.getId() != null) {
            throw new IllegalArgumentException("Cannot save user that already has an id");
        }
        try (Connection connection = databaseConnectionManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(INSERT_USER_SQL)) {
            statement.setString(1, user.getFirstName());
            statement.setString(2, user.getLastName());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new SQLException("Insert did not return a generated user id");
                }
                long userId = resultSet.getLong(USER_ID_COLUMN);
                return User.reconstitute(userId, user.getFirstName(), user.getLastName());
            }
        } catch (SQLException exception) {
            throw new PersistenceException("Failed to save user", exception);
        }
    }

    @Override
    public Optional<User> findById(long id) {
        if (id <= 0) {
            throw new IllegalArgumentException("id must be positive");
        }
        try (Connection connection = databaseConnectionManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(FIND_USER_BY_ID_SQL)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapUser(resultSet));
            }
        } catch (SQLException exception) {
            throw new PersistenceException("Unable to find user by id", exception);
        }
    }

    @Override
    public List<User> findAll() {
        try (Connection connection = databaseConnectionManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(FIND_ALL_SQL)) {
            List<User> users = new ArrayList<>();
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    users.add(mapUser(resultSet));
                }
            }
            return users;
        } catch (SQLException exception) {
            throw new PersistenceException("Unable to find users", exception);
        }
    }

    @Override
    public User update(User user) {
        Objects.requireNonNull(user, "user must not be null");
        if (user.getId() == null) {
            throw new IllegalArgumentException("userId cannot be null");
        }
        try (Connection connection = databaseConnectionManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(UPDATE_SQL)) {
            statement.setString(1, user.getFirstName());
            statement.setString(2, user.getLastName());
            statement.setLong(3, user.getId());

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new IllegalArgumentException("User does not exist with id: " + user.getId());
                }
                return mapUser(resultSet);
            }
        } catch (SQLException exception) {
            throw new PersistenceException("Unable to update user", exception);
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
            throw new PersistenceException("Unable to delete user", exception);
        }
    }

    private static User mapUser(ResultSet resultSet) throws SQLException {
        long userId = resultSet.getLong(USER_ID_COLUMN);
        String firstName = resultSet.getString(FIRST_NAME_COLUMN);
        String lastName = resultSet.getString(LAST_NAME_COLUMN);
        return User.reconstitute(userId, firstName, lastName);
    }
}
