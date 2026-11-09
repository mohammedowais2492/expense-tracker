package com.expensetracker.persistence.repositories;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.expensetracker.models.FinancialTransaction;
import com.expensetracker.models.FinancialTransactionQuery;
import com.expensetracker.models.Necessity;
import com.expensetracker.models.Page;
import com.expensetracker.models.PageRequest;
import com.expensetracker.models.TransactionType;
import com.expensetracker.persistence.DatabaseConnectionManager;
import com.expensetracker.persistence.exceptions.PersistenceException;
import com.expensetracker.repositories.FinancialTransactionRepository;

/**
 * SQLite-backed implementation of {@link FinancialTransactionRepository}.
 *
 * <p>
 * Provides persistence operations for financial transactions, including
 * creation, retrieval, updating, and deletion. Supports transaction filtering
 * and keyset pagination.
 *
 * <p>
 * Uses {@link DatabaseConnectionManager} to obtain JDBC connections and
 * translates SQL exceptions into {@link PersistenceException}.
 */
public class SQLiteFinancialTransactionRepository implements FinancialTransactionRepository {
    private static final String TRANSACTION_ID_COLUMN = "TransactionId";
    private static final String PROFILE_ID_COLUMN = "ProfileId";
    private static final String CATEGORY_ID_COLUMN = "CategoryId";
    private static final String TYPE_COLUMN = "Type";
    private static final String AMOUNT_COLUMN = "Amount";
    private static final String DESCRIPTION_COLUMN = "Description";
    private static final String TRANSACTION_DATE_COLUMN = "TransactionDate";
    private static final String NECESSITY_COLUMN = "Necessity";

    private static final String INSERT_SQL = String.format("""
                INSERT INTO FinancialTransaction (%s, %s, %s, %s, %s, %s, %s)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                RETURNING %s
            """, PROFILE_ID_COLUMN, CATEGORY_ID_COLUMN, TYPE_COLUMN, AMOUNT_COLUMN, DESCRIPTION_COLUMN,
            TRANSACTION_DATE_COLUMN, NECESSITY_COLUMN, TRANSACTION_ID_COLUMN);
    private static final String FIND_BY_ID_SQL = String.format("""
                SELECT %s, %s, %s, %s, %s, %s, %s, %s
                FROM FinancialTransaction
                WHERE %s = ?
            """, TRANSACTION_ID_COLUMN, PROFILE_ID_COLUMN, CATEGORY_ID_COLUMN, TYPE_COLUMN, AMOUNT_COLUMN,
            DESCRIPTION_COLUMN, TRANSACTION_DATE_COLUMN, NECESSITY_COLUMN, TRANSACTION_ID_COLUMN);
    private static final String FIND_ALL_SQL_BASE_QUERY = String.format("""
                SELECT %s, %s, %s, %s, %s, %s, %s, %s
                FROM FinancialTransaction
                WHERE %s = ? AND %s = ?
            """, TRANSACTION_ID_COLUMN, PROFILE_ID_COLUMN, CATEGORY_ID_COLUMN, TYPE_COLUMN, AMOUNT_COLUMN,
            DESCRIPTION_COLUMN, TRANSACTION_DATE_COLUMN, NECESSITY_COLUMN, PROFILE_ID_COLUMN, TYPE_COLUMN);
    private static final String UPDATE_SQL = String.format("""
            UPDATE FinancialTransaction
            SET %s = ?, %s = ?, %s = ?, %s = ?,
                %s = ?, %s = ?, %s = ?
            WHERE %s = ?
            RETURNING %s, %s, %s, %s, %s, %s, %s, %s
            """,
            PROFILE_ID_COLUMN, CATEGORY_ID_COLUMN, TYPE_COLUMN, AMOUNT_COLUMN, DESCRIPTION_COLUMN,
            TRANSACTION_DATE_COLUMN, NECESSITY_COLUMN, TRANSACTION_ID_COLUMN, TRANSACTION_ID_COLUMN,
            PROFILE_ID_COLUMN, CATEGORY_ID_COLUMN, TYPE_COLUMN, AMOUNT_COLUMN, DESCRIPTION_COLUMN,
            TRANSACTION_DATE_COLUMN, NECESSITY_COLUMN);
    private static final String DELETE_BY_ID_SQL = String.format("""
            DELETE FROM FinancialTransaction
            WHERE %s = ?
            """, TRANSACTION_ID_COLUMN);

    private final DatabaseConnectionManager databaseConnectionManager;

    /**
     * Creates a repository using the specified database connection manager.
     *
     * @param databaseConnectionManager the manager responsible for providing
     *                                  database connections
     * @throws NullPointerException if {@code databaseConnectionManager} is null
     */
    public SQLiteFinancialTransactionRepository(DatabaseConnectionManager databaseConnectionManager) {
        this.databaseConnectionManager = Objects.requireNonNull(databaseConnectionManager,
                "databaseConnectionManager must not be null");
    }

    @Override
    public FinancialTransaction save(FinancialTransaction transaction) {
        Objects.requireNonNull(transaction, "transaction must not be null");
        if (transaction.getId() != null) {
            throw new IllegalArgumentException("id must be null");
        }
        try (Connection connection = databaseConnectionManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(INSERT_SQL)) {
            statement.setLong(1, transaction.getProfileId());
            statement.setLong(2, transaction.getCategoryId());
            statement.setString(3, transaction.getType().name());
            statement.setLong(4, transaction.getAmount());
            if (transaction.getDescription() == null) {
                statement.setNull(5, Types.VARCHAR);
            } else {
                statement.setString(5, transaction.getDescription());
            }
            statement.setString(6, transaction.getTransactionDate().format(DateTimeFormatter.ISO_DATE));
            if (transaction.getNecessity() == null) {
                statement.setNull(7, Types.VARCHAR);
            } else {
                statement.setString(7, transaction.getNecessity().name());
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new SQLException("Insert did not return a generated transactionId");
                }
                return FinancialTransaction.reconstitute(
                        resultSet.getLong(TRANSACTION_ID_COLUMN),
                        transaction.getProfileId(),
                        transaction.getCategoryId(),
                        transaction.getAmount(),
                        transaction.getType(),
                        transaction.getTransactionDate(),
                        transaction.getDescription(),
                        transaction.getNecessity());
            }
        } catch (SQLException exception) {
            throw new PersistenceException("FinancialTransaction could not be created", exception);
        }
    }

    @Override
    public Optional<FinancialTransaction> findById(long id) {
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
                return Optional.of(mapFinancialTransaction(resultSet));
            }
        } catch (SQLException exception) {
            throw new PersistenceException("Unable to find financial transaction by id", exception);
        }
    }

    @Override
    public Page<FinancialTransaction> findAll(FinancialTransactionQuery query, PageRequest pageRequest) {
        Objects.requireNonNull(query, "query must not be null");
        Objects.requireNonNull(pageRequest, "pageRequest must not be null");
        LocalDate cursorDate = null;
        long cursorId = 0;
        int pageSize = pageRequest.getPageSize();
        String pageToken = pageRequest.getPageToken();

        if (pageToken != null) {
            String[] keys = pageToken.split(":", -1);
            if (keys.length != 2) {
                throw new IllegalArgumentException("Invalid page token");
            }
            try {
                cursorDate = LocalDate.parse(keys[0]);
                cursorId = Long.parseLong(keys[1]);
            } catch (DateTimeException | NumberFormatException exception) {
                throw new IllegalArgumentException("Invalid page token", exception);
            }
            if (cursorId <= 0) {
                throw new IllegalArgumentException("Invalid page token");
            }
        }

        String finalQuery = buildQuery(query, pageRequest);
        try (Connection connection = databaseConnectionManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(finalQuery)) {
            statement.setLong(1, query.getProfileId());
            statement.setString(2, query.getType().name());
            int index = 3;
            if (query.getCategoryId() != null) {
                statement.setLong(index++, query.getCategoryId());
            }
            if (query.getNecessity() != null) {
                statement.setString(index++, query.getNecessity().name());
            }
            if (query.getStartDate() != null) {
                statement.setString(index++, query.getStartDate().format(DateTimeFormatter.ISO_DATE));
            }
            if (query.getEndDate() != null) {
                statement.setString(index++, query.getEndDate().format(DateTimeFormatter.ISO_DATE));
            }
            if (pageRequest.getPageToken() != null) {
                statement.setString(index++, cursorDate.toString());
                statement.setLong(index++, cursorId);
            }
            statement.setInt(index++, pageSize + 1);
            try (ResultSet resultSet = statement.executeQuery()) {
                return mapPage(resultSet, pageSize);
            }
        } catch (SQLException exception) {
            throw new PersistenceException("Unable to find transactions", exception);
        }
    }

    @Override
    public FinancialTransaction update(FinancialTransaction transaction) {
        Objects.requireNonNull(transaction, "transaction must not be null");
        if (transaction.getId() == null) {
            throw new IllegalArgumentException("transactionId cannot be null");
        }
        try (Connection connection = databaseConnectionManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(UPDATE_SQL)) {
            statement.setLong(1, transaction.getProfileId());
            statement.setLong(2, transaction.getCategoryId());
            statement.setString(3, transaction.getType().name());
            statement.setLong(4, transaction.getAmount());
            if (transaction.getDescription() != null) {
                statement.setString(5, transaction.getDescription());
            } else {
                statement.setNull(5, Types.VARCHAR);
            }
            statement.setString(6, transaction.getTransactionDate().format(DateTimeFormatter.ISO_DATE));
            if (transaction.getNecessity() != null) {
                statement.setString(7, transaction.getNecessity().name());
            } else {
                statement.setNull(7, Types.VARCHAR);
            }
            statement.setLong(8, transaction.getId());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new IllegalArgumentException(
                            "FinancialTransaction does not exist with id: " + transaction.getId());
                }
                return mapFinancialTransaction(resultSet);
            }
        } catch (SQLException exception) {
            throw new PersistenceException("Unable to update financial transaction", exception);
        }
    }

    @Override
    public boolean deleteById(long id) {
        if (id <= 0) {
            throw new IllegalArgumentException("id must be positive");
        }
        try (Connection connection = databaseConnectionManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(DELETE_BY_ID_SQL)) {
            statement.setLong(1, id);
            return statement.executeUpdate() == 1;
        } catch (SQLException exception) {
            throw new PersistenceException("Unable to delete transaction", exception);
        }
    }

    private static FinancialTransaction mapFinancialTransaction(ResultSet resultSet) throws SQLException {
        long id = resultSet.getLong(TRANSACTION_ID_COLUMN);
        long profileId = resultSet.getLong(PROFILE_ID_COLUMN);
        long categoryId = resultSet.getLong(CATEGORY_ID_COLUMN);
        TransactionType type = TransactionType.valueOf(resultSet.getString(TYPE_COLUMN));
        long amount = resultSet.getLong(AMOUNT_COLUMN);
        String description = resultSet.getString(DESCRIPTION_COLUMN);
        LocalDate date = LocalDate.parse(resultSet.getString(TRANSACTION_DATE_COLUMN));
        String necessityValue = resultSet.getString(NECESSITY_COLUMN);
        Necessity necessity = necessityValue == null
                ? null
                : Necessity.valueOf(necessityValue);
        return FinancialTransaction.reconstitute(id, profileId, categoryId, amount, type, date, description, necessity);
    }

    private static Page<FinancialTransaction> mapPage(ResultSet resultSet, int pageSize) throws SQLException {
        Page.PageBuilder<FinancialTransaction> page = Page.<FinancialTransaction>builder().pageSize(pageSize);
        List<FinancialTransaction> content = new ArrayList<>();
        while (content.size() < pageSize && resultSet.next()) {
            content.add(mapFinancialTransaction(resultSet));
        }
        boolean hasNext = resultSet.next();

        String nextPageToken = null;

        if (hasNext) {
            FinancialTransaction lastTransaction = content.get(content.size() - 1);

            nextPageToken = lastTransaction.getTransactionDate() + ":"
                    + lastTransaction.getId();
        }
        page.hasNext(hasNext);
        page.nextPageToken(nextPageToken);
        page.content(content);
        return page.build();
    }

    private static String buildQuery(FinancialTransactionQuery query, PageRequest pageRequest) {
        StringBuilder queryBuilder = new StringBuilder(FIND_ALL_SQL_BASE_QUERY);
        if (query.getCategoryId() != null) {
            queryBuilder.append(String.format(" AND %s = ? ", CATEGORY_ID_COLUMN));
        }
        if (query.getNecessity() != null) {
            queryBuilder.append(String.format(" AND %s = ? ", NECESSITY_COLUMN));
        }
        if (query.getStartDate() != null) {
            queryBuilder.append(String.format(" AND %s >= ? ", TRANSACTION_DATE_COLUMN));
        }
        if (query.getEndDate() != null) {
            queryBuilder.append(String.format(" AND %s <= ? ", TRANSACTION_DATE_COLUMN));
        }
        if (pageRequest.getPageToken() != null) {
            queryBuilder.append(String.format(
                    " AND (%s, %s) < (?, ?)",
                    TRANSACTION_DATE_COLUMN,
                    TRANSACTION_ID_COLUMN));
        }
        queryBuilder.append(String.format("""
                 ORDER BY %s DESC, %s DESC
                 LIMIT ?
                """,
                TRANSACTION_DATE_COLUMN,
                TRANSACTION_ID_COLUMN));
        return queryBuilder.toString();
    }
}
