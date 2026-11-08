package com.expensetracker.persistence.repositories;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.expensetracker.models.Category;
import com.expensetracker.models.TransactionType;
import com.expensetracker.persistence.DatabaseConnectionManager;
import com.expensetracker.persistence.exceptions.PersistenceException;
import com.expensetracker.repositories.CategoryRepository;

/**
 * SQLite-backed implementation of {@link CategoryRepository}.
 *
 * <p>
 * Uses JDBC to persist and retrieve {@link Category} instances.
 * Database connections are obtained from the injected
 * {@link DatabaseConnectionManager}.
 * </p>
 */
public class SQLiteCategoryRepository implements CategoryRepository {
    private static final String CATEGORY_ID_COLUMN = "CategoryId";
    private static final String NAME_COLUMN = "Name";
    private static final String TYPE_COLUMN = "Type";
    private static final String IS_ACTIVE_COLUMN = "IsActive";

    private static final String INSERT_SQL = String.format("""
                INSERT INTO Category (%s, %s, %s)
                VALUES (?, ?, ?)
                RETURNING %s
            """, NAME_COLUMN, TYPE_COLUMN, IS_ACTIVE_COLUMN, CATEGORY_ID_COLUMN);
    private static final String FIND_BY_ID_SQL = String.format("""
                SELECT %s, %s, %s, %s
                FROM Category
                WHERE %s = ?
            """, CATEGORY_ID_COLUMN, NAME_COLUMN, TYPE_COLUMN, IS_ACTIVE_COLUMN, CATEGORY_ID_COLUMN);
    private static final String FIND_ALL_BY_TYPE_SQL = String.format("""
                SELECT %s, %s, %s, %s
                FROM Category
                WHERE %s = ?
                ORDER BY %s
            """, CATEGORY_ID_COLUMN, NAME_COLUMN, TYPE_COLUMN, IS_ACTIVE_COLUMN, TYPE_COLUMN, CATEGORY_ID_COLUMN);
    private static final String UPDATE_SQL = String.format("""
                UPDATE Category
                SET %s = ?, %s = ?
                WHERE %s = ?
                RETURNING %s, %s, %s, %s
            """, NAME_COLUMN, IS_ACTIVE_COLUMN, CATEGORY_ID_COLUMN, CATEGORY_ID_COLUMN, NAME_COLUMN, TYPE_COLUMN,
            IS_ACTIVE_COLUMN);

    private final DatabaseConnectionManager databaseConnectionManager;

    /**
     * Creates a SQLite-backed category repository using the specified
     * database connection manager.
     *
     * @param databaseConnectionManager the manager used to obtain
     *                                  database connections; must not be null
     * @throws NullPointerException if databaseConnectionManager is null
     */
    public SQLiteCategoryRepository(DatabaseConnectionManager databaseConnectionManager) {
        this.databaseConnectionManager = Objects.requireNonNull(databaseConnectionManager,
                "databaseConnectionManager must not be null");
    }

    @Override
    public Category save(Category category) {
        Objects.requireNonNull(category, "category must not be null");
        if (category.getId() != null) {
            throw new IllegalArgumentException("categoryId must be null");
        }
        try (Connection connection = databaseConnectionManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(INSERT_SQL)) {
            statement.setString(1, category.getName());
            statement.setString(2, category.getType().name());
            statement.setBoolean(3, category.isActive());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new SQLException("Insert did not generate a categoryId");
                }
                long id = resultSet.getLong(CATEGORY_ID_COLUMN);
                return Category.reconstitute(id, category.getName(), category.getType(), category.isActive());
            }
        } catch (SQLException exception) {
            throw new PersistenceException("Category could not be created", exception);
        }
    }

    @Override
    public Optional<Category> findById(long id) {
        if (id <= 0) {
            throw new IllegalArgumentException("id must be positive");
        }
        try (Connection connection = databaseConnectionManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(FIND_BY_ID_SQL)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapCategory(resultSet));
            }
        } catch (SQLException exception) {
            throw new PersistenceException("Unable to find category by categoryId", exception);
        }
    }

    @Override
    public List<Category> findAllByType(TransactionType type) {
        Objects.requireNonNull(type, "type must not be null");
        try (Connection connection = databaseConnectionManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(FIND_ALL_BY_TYPE_SQL)) {
            List<Category> categories = new ArrayList<>();
            statement.setString(1, type.name());
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    categories.add(mapCategory(resultSet));
                }
            }
            return categories;
        } catch (SQLException exception) {
            throw new PersistenceException("Unable to find categories by type", exception);
        }
    }

    @Override
    public Category update(Category category) {
        Objects.requireNonNull(category, "category must not be null");
        if (category.getId() == null) {
            throw new IllegalArgumentException("categoryId cannot be null");
        }
        try (Connection connection = databaseConnectionManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(UPDATE_SQL)) {
            statement.setString(1, category.getName());
            statement.setBoolean(2, category.isActive());
            statement.setLong(3, category.getId());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new IllegalArgumentException("category does not exist with id: " + category.getId());
                }
                return mapCategory(resultSet);
            }
        } catch (SQLException exception) {
            throw new PersistenceException("Unable to update category", exception);
        }
    }

    private static Category mapCategory(ResultSet resultSet) throws SQLException {
        long id = resultSet.getLong(CATEGORY_ID_COLUMN);
        String name = resultSet.getString(NAME_COLUMN);
        TransactionType type = TransactionType.valueOf(resultSet.getString(TYPE_COLUMN));
        boolean isActive = resultSet.getBoolean(IS_ACTIVE_COLUMN);
        return Category.reconstitute(id, name, type, isActive);
    }
}
