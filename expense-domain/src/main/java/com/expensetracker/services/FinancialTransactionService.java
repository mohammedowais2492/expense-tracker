package com.expensetracker.services;

import java.time.YearMonth;
import java.util.Optional;

import com.expensetracker.models.FinancialTransaction;
import com.expensetracker.models.FinancialTransactionQuery;
import com.expensetracker.models.Page;
import com.expensetracker.models.PageRequest;
import com.expensetracker.models.TransactionType;

/**
 * Defines application-level operations for managing financial transactions.
 *
 * <p>
 * Provides functionality for creating, retrieving, filtering, updating,
 * and deleting income and expense transactions belonging to expense profiles.
 *
 * <p>
 * Transactions can be retrieved by type, necessity classification,
 * calendar month, calendar year, or custom filtering criteria.
 * Collection-based retrieval operations return paginated results.
 *
 * <p>
 * Income and expense transactions are supported by this service,
 * although the initial application UI exposes expense tracking only.
 *
 * <p>
 * New transactions must reference an existing, active category whose
 * transaction type matches the transaction being created.
 */
public interface FinancialTransactionService {

        /**
         * Creates a new financial transaction.
         *
         * <p>
         * The transaction must reference an existing expense profile and
         * an active category matching its transaction type.
         *
         * <p>
         * Expense transactions require a necessity classification, while
         * income transactions must not have one.
         *
         * @param transaction the transaction to create; must not be null
         *                    and must not have an ID
         * @return the created transaction, including its generated ID
         * @throws NullPointerException     if transaction is null
         * @throws IllegalArgumentException if transaction already has an ID
         *                                  or references an invalid category
         *                                  or expense profile
         */
        FinancialTransaction createFinancialTransaction(
                        FinancialTransaction transaction);

        /**
         * Retrieves a financial transaction by its ID.
         *
         * @param transactionId the ID of the transaction; must be positive
         * @return an Optional containing the transaction if found,
         *         or an empty Optional otherwise
         * @throws IllegalArgumentException if transactionId is not positive
         */
        Optional<FinancialTransaction> getFinancialTransactionById(
                        long transactionId);

        /**
         * Retrieves transactions of a specified type for an expense profile.
         *
         * @param profileId   the ID of the expense profile; must be positive
         * @param type        the transaction type; must not be null
         * @param pageRequest the pagination parameters; must not be null
         * @return a page of transactions matching the specified type
         * @throws IllegalArgumentException if profileId is not positive
         * @throws NullPointerException     if type or pageRequest is null
         */
        Page<FinancialTransaction> getFinancialTransactionsOfType(
                        long profileId,
                        TransactionType type,
                        PageRequest pageRequest);

        /**
         * Retrieves necessary expenses belonging to an expense profile.
         *
         * <p>
         * Only expense transactions classified as necessary are returned.
         *
         * @param profileId   the ID of the expense profile; must be positive
         * @param pageRequest the pagination parameters; must not be null
         * @return a page of necessary expenses
         * @throws IllegalArgumentException if profileId is not positive
         * @throws NullPointerException     if pageRequest is null
         */
        Page<FinancialTransaction> getNecessaryExpenses(
                        long profileId,
                        PageRequest pageRequest);

        /**
         * Retrieves unnecessary expenses belonging to an expense profile.
         *
         * <p>
         * Only expense transactions classified as unnecessary are returned.
         *
         * @param profileId   the ID of the expense profile; must be positive
         * @param pageRequest the pagination parameters; must not be null
         * @return a page of unnecessary expenses
         * @throws IllegalArgumentException if profileId is not positive
         * @throws NullPointerException     if pageRequest is null
         */
        Page<FinancialTransaction> getUnnecessaryExpenses(
                        long profileId,
                        PageRequest pageRequest);

        /**
         * Retrieves financial transactions for a specified calendar month.
         *
         * <p>
         * The date range includes the first and last days of the month.
         * In version 1, only expense transactions are returned.
         *
         * @param profileId   the ID of the expense profile; must be positive
         * @param month       the calendar month and year; must not be null
         * @param pageRequest the pagination parameters; must not be null
         * @return a page of transactions occurring during the specified month
         * @throws IllegalArgumentException if profileId is not positive
         * @throws NullPointerException     if month or pageRequest is null
         */
        Page<FinancialTransaction> getFinancialTransactionsForMonth(
                        long profileId,
                        YearMonth month,
                        PageRequest pageRequest);

        /**
         * Retrieves financial transactions for a specified calendar year.
         *
         * <p>
         * The date range includes January 1 through December 31,
         * inclusive. In version 1, only expense transactions are returned.
         *
         * @param profileId   the ID of the expense profile; must be positive
         * @param year        the calendar year to retrieve
         * @param pageRequest the pagination parameters; must not be null
         * @return a page of transactions occurring during the specified year
         * @throws IllegalArgumentException if profileId is not positive
         * @throws NullPointerException     if pageRequest is null
         */
        Page<FinancialTransaction> getFinancialTransactionsForYear(
                        long profileId,
                        int year,
                        PageRequest pageRequest);

        /**
         * Retrieves financial transactions matching custom filtering criteria.
         *
         * <p>
         * The query supports filtering by expense profile, transaction type,
         * category, necessity classification, and an inclusive date range.
         * Transaction type is mandatory in version 1.
         *
         * <p>
         * Results are ordered by transaction date descending, followed by
         * transaction ID descending.
         *
         * @param query       the filtering criteria; must not be null
         * @param pageRequest the pagination parameters; must not be null
         * @return a page of transactions matching the specified criteria
         * @throws NullPointerException if query or pageRequest is null
         */
        Page<FinancialTransaction> getFilteredFinancialTransactions(
                        FinancialTransactionQuery query,
                        PageRequest pageRequest);

        /**
         * Updates an existing financial transaction.
         *
         * <p>
         * The transaction must have an ID and reference an existing
         * expense profile and category.
         *
         * <p>
         * Both income and expense transactions are supported. The category's
         * transaction type must match the transaction's type. Expense transactions
         * require a necessity classification, while income transactions must not
         * have one.
         *
         * <p>
         * An existing transaction may retain an inactive category when
         * updating other fields. However, assigning a different category
         * requires the new category to be active.
         *
         * <p>
         * A transaction may be moved to another existing expense profile.
         * Its transaction ID remains unchanged.
         *
         * @param transaction the transaction containing updated information;
         *                    must not be null and must have an ID
         * @return the updated transaction
         * @throws NullPointerException     if transaction is null
         * @throws IllegalArgumentException if transaction has no ID,
         *                                  does not exist, or violates
         *                                  transaction business rules
         */
        FinancialTransaction updateTransaction(
                        FinancialTransaction transaction);

        /**
         * Deletes a financial transaction by its ID.
         *
         * @param transactionId the ID of the transaction; must be positive
         * @return true if the transaction was deleted, or false if no transaction
         *         exists with the specified ID
         * @throws IllegalArgumentException if transactionId is not positive
         */
        boolean deleteTransaction(long transactionId);
}
