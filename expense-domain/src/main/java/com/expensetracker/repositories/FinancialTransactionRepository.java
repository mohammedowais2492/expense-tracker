
package com.expensetracker.repositories;

import java.util.Optional;

import com.expensetracker.models.FinancialTransaction;
import com.expensetracker.models.FinancialTransactionQuery;
import com.expensetracker.models.Page;
import com.expensetracker.models.PageRequest;

/**
 * Defines persistence operations for financial transactions.
 *
 * <p>
 * Implementations are responsible for storing, retrieving, updating, and
 * deleting financial transactions. Query operations support filtering and
 * cursor-based pagination through {@link FinancialTransactionQuery} and
 * {@link PageRequest}.
 * </p>
 *
 * <p>
 * Business rules and authorization checks, such as verifying profile
 * ownership or category activity, are the responsibility of the service layer.
 * </p>
 */
public interface FinancialTransactionRepository {

    /**
     * Persists a new financial transaction.
     *
     * @param transaction the transaction to persist; must not be null and must
     *                    not already have a persistence ID
     * @return the persisted transaction, including its generated ID
     * @throws IllegalArgumentException if the transaction already has an ID
     */
    FinancialTransaction save(FinancialTransaction transaction);

    /**
     * Retrieves a financial transaction by its unique ID.
     *
     * @param id the transaction ID; must be positive
     * @return an {@link Optional} containing the transaction if found,
     *         or an empty {@link Optional} otherwise
     * @throws IllegalArgumentException if the ID is not positive
     */
    Optional<FinancialTransaction> findById(long id);

    /**
     * Retrieves a page of financial transactions matching the specified query.
     *
     * <p>
     * Results are ordered by transaction date descending, followed by
     * transaction ID descending, providing a stable ordering for cursor-based
     * pagination.
     * </p>
     *
     * @param query       the transaction filtering criteria; must not be null
     * @param pageRequest the pagination request; must not be null
     * @return a page containing the matching transactions and pagination
     *         metadata; never null
     */
    Page<FinancialTransaction> findAll(
            FinancialTransactionQuery query,
            PageRequest pageRequest);

    /**
     * Updates an existing financial transaction.
     *
     * @param transaction the transaction containing the updated values;
     *                    must not be null and must have a persistence ID
     * @return the updated transaction
     * @throws IllegalArgumentException if the transaction has no persistence ID
     *                                  or does not exist
     */
    FinancialTransaction update(FinancialTransaction transaction);

    /**
     * Deletes a financial transaction by its unique ID.
     *
     * @param id the transaction ID; must be positive
     * @return {@code true} if a transaction was deleted, or {@code false}
     *         if no transaction with the specified ID exists
     * @throws IllegalArgumentException if the ID is not positive
     */
    boolean deleteById(long id);
}
