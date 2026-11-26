package com.expensetracker.models;

import lombok.Builder;
import lombok.Value;

/**
 * Represents aggregated metrics for a collection of financial transactions.
 *
 * <p>
 * Contains the total monetary amount and the number of transactions
 * contributing to the aggregation. The total amount is expressed in
 * cents, consistent with the application's monetary representation.
 * </p>
 *
 * <p>
 * This model is independent of the aggregation criteria and grouping
 * dimension. It can represent results aggregated across all matching
 * transactions or grouped by category, necessity, calendar month,
 * or other supported dimensions.
 * </p>
 *
 * <p>
 * Instances are immutable and are constructed using a builder.
 * </p>
 */
@Value
public class TransactionAggregate {
    long totalAmount;
    long transactionCount;

    /**
     * Creates an aggregation result with the specified metrics.
     *
     * @param totalAmount      the sum of transaction amounts, expressed in cents;
     *                         may be negative, zero, or positive
     * @param transactionCount the number of transactions contributing to
     *                         the aggregation; must not be negative
     * @throws IllegalArgumentException if transactionCount is negative
     */
    @Builder
    public TransactionAggregate(long totalAmount, long transactionCount) {
        if (transactionCount < 0) {
            throw new IllegalArgumentException("transactionCount must not be negative");
        }
        this.totalAmount = totalAmount;
        this.transactionCount = transactionCount;
    }
}
