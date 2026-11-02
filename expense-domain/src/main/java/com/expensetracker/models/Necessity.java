package com.expensetracker.models;

/**
 * Classifies an expense according to whether it was considered necessary.
 *
 * <p>
 * This classification applies only to {@link TransactionType#EXPENSE}
 * transactions. Income transactions must not have a necessity classification.
 */
public enum Necessity {
    /** Represents an expense considered necessary. */
    NECESSARY,
    /** Represents an expense considered unnecessary. */
    UNNECESSARY
}
