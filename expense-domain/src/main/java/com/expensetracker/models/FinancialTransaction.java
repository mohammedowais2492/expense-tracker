package com.expensetracker.models;

import java.time.LocalDate;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

/**
 * Represents a financial transaction associated with an expense profile
 * and a category.
 *
 * <p>
 * A transaction can represent either income or an expense. Expenses
 * must specify a {@link Necessity}, while income transactions must not.
 *
 * <p>
 * Amounts are represented as positive integer values in the smallest
 * currency unit (for example, cents), avoiding floating-point precision
 * issues.
 *
 * <p>
 * A transaction created through the builder has no persistence ID.
 * Transactions loaded from persistence can be reconstructed using
 * {@link #reconstitute(long, long, long, long, TransactionType, LocalDate, String, Necessity)}.
 */
@Value
public class FinancialTransaction {
    long profileId;
    long categoryId;
    long amount;
    TransactionType type;
    LocalDate transactionDate;

    Long id;
    String description;
    Necessity necessity;

    /**
     * Creates a new financial transaction.
     *
     * <p>
     * The transaction is initially unpersisted and therefore has no ID.
     *
     * @param profileId       the ID of the expense profile associated with this
     *                        transaction
     * @param categoryId      the ID of the category associated with this
     *                        transaction
     * @param amount          the positive transaction amount in the smallest
     *                        currency unit
     * @param type            the transaction type; must not be {@code null}
     * @param transactionDate the date on which the transaction occurred; must not
     *                        be {@code null}
     * @param description     an optional description; blank descriptions are
     *                        normalized to {@code null}
     * @param necessity       the necessity classification; required for expenses
     *                        and must be {@code null} for income
     * @throws IllegalArgumentException if an ID or amount is not positive, or if
     *                                  the necessity
     *                                  classification is inconsistent with the
     *                                  transaction type
     * @throws NullPointerException     if {@code type} or {@code transactionDate}
     *                                  is {@code null}
     */
    @Builder
    public FinancialTransaction(
            long profileId,
            long categoryId,
            long amount,
            @NonNull TransactionType type,
            @NonNull LocalDate transactionDate,
            String description,
            Necessity necessity) {
        this(null, profileId, categoryId, amount, type, transactionDate, description, necessity);
    }

    private FinancialTransaction(
            Long id,
            long profileId,
            long categoryId,
            long amount,
            TransactionType type,
            LocalDate transactionDate,
            String description,
            Necessity necessity) {
        if (id != null && id <= 0) {
            throw new IllegalArgumentException("id cannot be <=0");
        }
        this.id = id;
        if (profileId <= 0) {
            throw new IllegalArgumentException("profileId cannot be <=0");
        }
        this.profileId = profileId;
        if (categoryId <= 0) {
            throw new IllegalArgumentException("categoryId cannot be <=0");
        }
        this.categoryId = categoryId;
        if (amount <= 0) {
            throw new IllegalArgumentException("amount cannot be <=0");
        }
        this.amount = amount;
        this.type = type;
        this.transactionDate = transactionDate;
        if (description != null && description.isBlank()) {
            description = null;
        }
        this.description = description;
        if (type == TransactionType.EXPENSE && necessity == null) {
            throw new IllegalArgumentException("necessity is required for expense");
        }
        if (type == TransactionType.INCOME && necessity != null) {
            throw new IllegalArgumentException("necessity should not be specified for income");
        }
        this.necessity = necessity;
    }

    /**
     * Reconstructs a financial transaction using data retrieved from persistence.
     *
     * <p>
     * Unlike the builder, this factory method accepts an existing persistence ID.
     * It applies the same validation rules as creating a new transaction.
     *
     * @param id              the positive persistence ID of the transaction
     * @param profileId       the ID of the associated expense profile
     * @param categoryId      the ID of the associated category
     * @param amount          the positive transaction amount in the smallest
     *                        currency unit
     * @param type            the transaction type; must not be {@code null}
     * @param transactionDate the date on which the transaction occurred; must not
     *                        be {@code null}
     * @param description     an optional description; blank descriptions are
     *                        normalized to {@code null}
     * @param necessity       the necessity classification; required for expenses
     *                        and must be {@code null} for income
     * @return a reconstructed financial transaction
     * @throws IllegalArgumentException if an ID or amount is not positive, or if
     *                                  the necessity
     *                                  classification is inconsistent with the
     *                                  transaction type
     * @throws NullPointerException     if {@code type} or {@code transactionDate}
     *                                  is {@code null}
     */
    public static FinancialTransaction reconstitute(
            long id,
            long profileId,
            long categoryId,
            long amount,
            @NonNull TransactionType type,
            @NonNull LocalDate transactionDate,
            String description,
            Necessity necessity) {
        return new FinancialTransaction(id, profileId, categoryId, amount, type, transactionDate,
                description, necessity);
    }
}
