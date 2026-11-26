package com.expensetracker.repositories;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;
import java.util.Optional;

import com.expensetracker.models.FinancialTransaction;
import com.expensetracker.models.FinancialTransactionQuery;
import com.expensetracker.models.Necessity;
import com.expensetracker.models.Page;
import com.expensetracker.models.PageRequest;
import com.expensetracker.models.TransactionAggregate;
import com.expensetracker.models.TransactionType;

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

        /**
         * Calculates the total expense amount for an expense profile
         * within a specified date range.
         *
         * <p>
         * The date range is start-inclusive and end-exclusive.
         * Only transactions of type
         * {@link com.expensetracker.models.TransactionType#EXPENSE}
         * are included in the aggregation.
         *
         * <p>
         * The aggregation is performed across all matching transactions,
         * independently of pagination. Returns {@code 0L} when no matching transactions
         * exist.
         *
         * @param profileId the ID of the expense profile; must be positive
         * @param startDate the inclusive start date; must not be null
         * @param endDate   the exclusive end date; must not be null
         *                  and must be after startDate
         * @return the total expense amount for the specified date range,
         *         or zero if no matching expenses exist
         * @throws IllegalArgumentException if profileId is not positive
         *                                  or endDate is not after startDate
         * @throws NullPointerException     if startDate or endDate is null
         */
        long getTotalExpenses(
                        long profileId,
                        LocalDate startDate,
                        LocalDate endDate);

        /**
         * Aggregates financial transactions matching the specified query,
         * grouping the results by category.
         *
         * <p>
         * Each entry in the returned map associates a category ID with the
         * aggregated metrics of all matching transactions belonging to that
         * category. Aggregation is performed across all matching transactions,
         * independently of pagination.
         * </p>
         *
         * <p>
         * All filtering criteria specified by {@link FinancialTransactionQuery}
         * are applied, including transaction type, category, necessity, and
         * optional date boundaries. Date boundaries follow the inclusive
         * semantics defined by the query.
         * </p>
         *
         * <p>
         * Total amounts are expressed in cents. Categories with no matching
         * transactions are omitted from the result. An empty map is returned
         * when no transactions match the query.
         * </p>
         *
         * @param query the transaction filtering criteria; must not be null
         * @return an unmodifiable map associating category IDs with their
         *         aggregated transaction metrics; never null
         * @throws NullPointerException if query is null
         */
        Map<Long, TransactionAggregate> aggregateByCategory(
                        FinancialTransactionQuery query);

        /**
         * Aggregates financial transactions matching the specified query.
         *
         * <p>
         * Calculates the total monetary amount and transaction count across all
         * matching transactions, independently of pagination.
         * </p>
         *
         * <p>
         * All filtering criteria defined by {@link FinancialTransactionQuery}
         * are applied, including transaction type, category, necessity, and
         * optional date boundaries. Date boundaries are inclusive.
         * </p>
         *
         * <p>
         * The total amount is expressed in cents. If no transactions match
         * the query, the returned aggregate contains a total amount of zero
         * and a transaction count of zero.
         * </p>
         *
         * @param query the transaction filtering criteria; must not be null
         * @return the aggregated transaction metrics; never null
         * @throws NullPointerException if query is null
         */
        TransactionAggregate aggregate(FinancialTransactionQuery query);

        /**
         * Aggregates expense transactions matching the specified query,
         * grouping the results by necessity classification.
         *
         * <p>
         * Each entry in the returned map associates a necessity classification
         * with the aggregated metrics of all matching expense transactions
         * belonging to that classification. Aggregation is performed across
         * all matching transactions, independently of pagination.
         * </p>
         *
         * <p>
         * All applicable filtering criteria defined by
         * {@link FinancialTransactionQuery} are applied, including category,
         * necessity, and optional date boundaries. Date boundaries are inclusive.
         * </p>
         *
         * <p>
         * This operation supports only expense transactions because income
         * transactions do not have a necessity classification.
         * </p>
         *
         * <p>
         * Total amounts are expressed in cents. Classifications with no
         * matching transactions are omitted. An empty map is returned
         * when no transactions match the query.
         * </p>
         *
         * @param query the expense transaction filtering criteria; must not be null
         *              and must specify {@link TransactionType#EXPENSE}
         * @return an unmodifiable map associating necessity classifications
         *         with their aggregated transaction metrics; never null
         * @throws NullPointerException     if query is null
         * @throws IllegalArgumentException if the query specifies a transaction
         *                                  type other than
         *                                  {@link TransactionType#EXPENSE}
         */
        Map<Necessity, TransactionAggregate> aggregateByNecessity(
                        FinancialTransactionQuery query);

        /**
         * Aggregates financial transactions matching the specified query,
         * grouping the results by calendar month.
         *
         * <p>
         * Each entry in the returned map associates a {@link YearMonth}
         * with the aggregated metrics of all matching transactions occurring
         * within that month. Aggregation is performed across all matching
         * transactions, independently of pagination.
         * </p>
         *
         * <p>
         * All filtering criteria defined by {@link FinancialTransactionQuery}
         * are applied, including transaction type, category, necessity, and
         * optional date boundaries. Date boundaries are inclusive.
         * </p>
         *
         * <p>
         * Total amounts are expressed in cents. Months without matching
         * transactions are omitted from the result. An empty map is returned
         * when no transactions match the query.
         * </p>
         *
         * @param query the transaction filtering criteria; must not be null
         * @return an unmodifiable map associating calendar months with their
         *         aggregated transaction metrics; never null
         * @throws NullPointerException if query is null
         */
        Map<YearMonth, TransactionAggregate> aggregateByMonth(
                        FinancialTransactionQuery query);

}
