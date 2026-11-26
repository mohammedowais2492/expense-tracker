package com.expensetracker.services;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;

import com.expensetracker.models.Necessity;
import com.expensetracker.models.TransactionAggregate;

/**
 * Defines application-level operations for generating financial reports.
 *
 * <p>
 * Provides aggregated expense information for an expense profile,
 * including overall summaries and breakdowns by category, necessity
 * classification, and calendar month.
 * </p>
 *
 * <p>
 * All reporting operations are scoped to a single expense profile and
 * include only expense transactions. Aggregations are calculated across
 * all matching transactions, independently of pagination.
 * </p>
 *
 * <p>
 * Date boundaries are inclusive and optional. A {@code null} start date
 * indicates no lower date boundary, while a {@code null} end date
 * indicates no upper date boundary. When both dates are provided,
 * the start date must not be after the end date.
 * </p>
 *
 * <p>
 * Monetary amounts are expressed in cents. Grouped aggregation methods
 * return unmodifiable maps containing only groups with matching
 * transactions.
 * </p>
 */
public interface FinancialReportService {

    /**
     * Retrieves aggregated expense metrics for an expense profile.
     *
     * <p>
     * Calculates the total expense amount and transaction count for
     * all expenses matching the specified date range.
     * </p>
     *
     * <p>
     * When no expenses match, the returned aggregate contains
     * a total amount of zero and a transaction count of zero.
     * </p>
     *
     * @param profileId the expense profile ID; must be positive
     * @param startDate the optional inclusive start date
     * @param endDate   the optional inclusive end date
     * @return the aggregated expense metrics; never null
     * @throws IllegalArgumentException if profileId is not positive or
     *                                  startDate is after endDate
     */
    TransactionAggregate getExpenseSummary(
            long profileId,
            LocalDate startDate,
            LocalDate endDate);

    /**
     * Retrieves aggregated expense metrics grouped by category.
     *
     * <p>
     * Each map entry associates a category ID with the total amount
     * and transaction count of expenses belonging to that category.
     * Categories without matching expenses are omitted.
     * </p>
     *
     * <p>
     * Inactive categories are included when historical transactions
     * reference them.
     * </p>
     *
     * @param profileId the expense profile ID; must be positive
     * @param startDate the optional inclusive start date
     * @param endDate   the optional inclusive end date
     * @return an unmodifiable map of category IDs to their aggregated
     *         expense metrics, or an empty map if no expenses match
     * @throws IllegalArgumentException if profileId is not positive or
     *                                  startDate is after endDate
     */
    Map<Long, TransactionAggregate> getExpensesByCategory(
            long profileId,
            LocalDate startDate,
            LocalDate endDate);

    /**
     * Retrieves aggregated expense metrics grouped by necessity.
     *
     * <p>
     * Each map entry associates a necessity classification with the
     * total amount and transaction count of matching expenses.
     * Classifications without matching expenses are omitted.
     * </p>
     *
     * @param profileId the expense profile ID; must be positive
     * @param startDate the optional inclusive start date
     * @param endDate   the optional inclusive end date
     * @return an unmodifiable map of necessity classifications to
     *         their aggregated expense metrics, or an empty map
     *         if no expenses match
     * @throws IllegalArgumentException if profileId is not positive or
     *                                  startDate is after endDate
     */
    Map<Necessity, TransactionAggregate> getExpensesByNecessity(
            long profileId,
            LocalDate startDate,
            LocalDate endDate);

    /**
     * Retrieves aggregated expense metrics grouped by calendar month.
     *
     * <p>
     * Each map entry associates a calendar month with the total amount
     * and transaction count of expenses occurring within that month.
     * Months without matching expenses are omitted.
     * </p>
     *
     * <p>
     * The returned map does not guarantee chronological iteration
     * order. Callers are responsible for sorting months when required
     * for presentation.
     * </p>
     *
     * @param profileId the expense profile ID; must be positive
     * @param startDate the optional inclusive start date
     * @param endDate   the optional inclusive end date
     * @return an unmodifiable map of calendar months to their
     *         aggregated expense metrics, or an empty map if
     *         no expenses match
     * @throws IllegalArgumentException if profileId is not positive or
     *                                  startDate is after endDate
     */
    Map<YearMonth, TransactionAggregate> getMonthlyExpenses(
            long profileId,
            LocalDate startDate,
            LocalDate endDate);
}
