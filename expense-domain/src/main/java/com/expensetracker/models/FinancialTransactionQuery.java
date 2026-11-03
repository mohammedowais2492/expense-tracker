package com.expensetracker.models;

import java.time.LocalDate;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

/**
 * Represents the criteria used to filter financial transactions.
 *
 * <p>
 * Every query is scoped to a single financial profile. Optional fields
 * allow callers to filter transactions by category, necessity, transaction
 * type, and date range.
 * </p>
 *
 * <p>
 * Date boundaries are inclusive. Either date boundary may be omitted:
 * a start date without an end date retrieves transactions on or after
 * the start date, while an end date without a start date retrieves
 * transactions on or before the end date.
 * </p>
 *
 * <p>
 * Necessity filtering applies only to expense transactions. A query
 * specifying {@link TransactionType#INCOME} must not specify a necessity.
 * </p>
 */
@Value
public class FinancialTransactionQuery {
    long profileId;
    Long categoryId;
    Necessity necessity;
    TransactionType type;
    LocalDate startDate;
    LocalDate endDate;

    /**
     * Creates a transaction query with the specified filtering criteria.
     *
     * @param profileId  the ID of the financial profile to query; must be positive
     * @param categoryId the optional category ID to filter by; must be positive
     *                   when provided
     * @param necessity  the optional necessity classification to filter by;
     *                   applicable only to expense transactions
     * @param type       the transaction type to filter by; must not be null
     * @param startDate  the optional inclusive lower date boundary
     * @param endDate    the optional inclusive upper date boundary
     * @throws IllegalArgumentException if profileId is not positive, categoryId
     *                                  is provided but not positive, necessity
     *                                  is provided for an income query, or
     *                                  startDate is after endDate
     * @throws NullPointerException     if type is null
     */
    @Builder
    public FinancialTransactionQuery(
            long profileId,
            Long categoryId,
            Necessity necessity,
            @NonNull TransactionType type,
            LocalDate startDate,
            LocalDate endDate) {
        if (profileId <= 0) {
            throw new IllegalArgumentException("profileId must be positive");
        }
        if (categoryId != null && categoryId <= 0) {
            throw new IllegalArgumentException("categoryId must be positive");
        }
        if (type.equals(TransactionType.INCOME) && necessity != null) {
            throw new IllegalArgumentException("necessity must be null for transaction of type INCOME");
        }
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("startDate must not be after endDate");
        }
        this.profileId = profileId;
        this.categoryId = categoryId;
        this.necessity = necessity;
        this.type = type;
        this.startDate = startDate;
        this.endDate = endDate;
    }
}
