package com.expensetracker.service.impl;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Objects;
import java.util.Optional;

import com.expensetracker.models.Category;
import com.expensetracker.models.FinancialTransaction;
import com.expensetracker.models.FinancialTransactionQuery;
import com.expensetracker.models.Necessity;
import com.expensetracker.models.Page;
import com.expensetracker.models.PageRequest;
import com.expensetracker.models.TransactionType;
import com.expensetracker.repositories.CategoryRepository;
import com.expensetracker.repositories.FinancialTransactionRepository;
import com.expensetracker.services.FinancialTransactionService;

/**
 * Default implementation of {@link FinancialTransactionService}.
 *
 * <p>
 * Provides application-level operations for managing financial
 * transactions, including validation of category assignments and
 * construction of queries for common retrieval operations.
 *
 * <p>
 * Delegates persistence to {@link FinancialTransactionRepository}
 * and category lookups to {@link CategoryRepository}.
 */
public class DefaultFinancialTransactionService implements FinancialTransactionService {
    private final CategoryRepository categoryRepository;
    private final FinancialTransactionRepository financialTransactionRepository;

    /**
     * Creates a financial transaction service backed by the specified
     * repositories.
     *
     * @param categoryRepository             the repository used to retrieve
     *                                       categories
     * @param financialTransactionRepository the repository used to manage
     *                                       financial transactions
     * @throws NullPointerException if either repository is null
     */
    public DefaultFinancialTransactionService(
            CategoryRepository categoryRepository,
            FinancialTransactionRepository financialTransactionRepository) {
        this.categoryRepository = Objects.requireNonNull(categoryRepository, "categoryRepository must not be null");
        this.financialTransactionRepository = Objects.requireNonNull(financialTransactionRepository,
                "financialTransactionRepository must not be null");
    }

    @Override
    public FinancialTransaction createFinancialTransaction(
            FinancialTransaction transaction) {

        Objects.requireNonNull(transaction, "transaction must not be null");

        if (transaction.getId() != null) {
            throw new IllegalArgumentException(
                    "New transaction must not have an id");
        }

        long categoryId = transaction.getCategoryId();

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Category does not exist with id: " + categoryId));

        if (!category.isActive()) {
            throw new IllegalArgumentException(
                    "Cannot create transaction with an inactive category");
        }

        if (category.getType() != transaction.getType()) {
            throw new IllegalArgumentException(
                    "Category type does not match transaction type");
        }

        return financialTransactionRepository.save(transaction);
    }

    @Override
    public Optional<FinancialTransaction> getFinancialTransactionById(long transactionId) {
        if (transactionId <= 0) {
            throw new IllegalArgumentException("transactionId must be positive");
        }
        return financialTransactionRepository.findById(transactionId);
    }

    @Override
    public Page<FinancialTransaction> getFinancialTransactionsOfType(long profileId, TransactionType type,
            PageRequest pageRequest) {
        if (profileId <= 0) {
            throw new IllegalArgumentException("profileId must be positive");
        }
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(pageRequest, "pageRequest must not be null");
        FinancialTransactionQuery query = prepareQuery(profileId, null, type, null, null);
        return financialTransactionRepository.findAll(query, pageRequest);
    }

    @Override
    public Page<FinancialTransaction> getNecessaryExpenses(long profileId, PageRequest pageRequest) {
        if (profileId <= 0) {
            throw new IllegalArgumentException("profileId must be positive");
        }
        Objects.requireNonNull(pageRequest, "pageRequest must not be null");
        FinancialTransactionQuery query = prepareQuery(profileId, Necessity.NECESSARY, TransactionType.EXPENSE,
                null, null);
        return financialTransactionRepository.findAll(query, pageRequest);
    }

    @Override
    public Page<FinancialTransaction> getUnnecessaryExpenses(long profileId, PageRequest pageRequest) {
        if (profileId <= 0) {
            throw new IllegalArgumentException("profileId must be positive");
        }
        Objects.requireNonNull(pageRequest, "pageRequest must not be null");
        FinancialTransactionQuery query = prepareQuery(profileId, Necessity.UNNECESSARY, TransactionType.EXPENSE,
                null, null);
        return financialTransactionRepository.findAll(query, pageRequest);
    }

    @Override
    public Page<FinancialTransaction> getFinancialTransactionsForMonth(long profileId, YearMonth month,
            PageRequest pageRequest) {
        Objects.requireNonNull(pageRequest, "pageRequest must not be null");
        Objects.requireNonNull(month, "month must not be null");
        FinancialTransactionQuery query = prepareQuery(profileId, null, TransactionType.EXPENSE,
                month.atDay(1),
                month.atEndOfMonth());
        return financialTransactionRepository.findAll(query, pageRequest);
    }

    @Override
    public Page<FinancialTransaction> getFinancialTransactionsForYear(long profileId, int year,
            PageRequest pageRequest) {
        Objects.requireNonNull(pageRequest, "pageRequest must not be null");
        FinancialTransactionQuery query = prepareQuery(profileId, null, TransactionType.EXPENSE,
                LocalDate.of(year, 1, 1),
                LocalDate.of(year, 12, 31));
        return financialTransactionRepository.findAll(query, pageRequest);
    }

    @Override
    public Page<FinancialTransaction> getFilteredFinancialTransactions(FinancialTransactionQuery query,
            PageRequest pageRequest) {
        Objects.requireNonNull(query, "financialTransactionQuery must not be null");
        Objects.requireNonNull(pageRequest, "pageRequest must not be null");
        return financialTransactionRepository.findAll(query, pageRequest);
    }

    @Override
    public FinancialTransaction updateTransaction(
            FinancialTransaction transaction) {

        Objects.requireNonNull(transaction, "transaction must not be null");

        if (transaction.getId() == null) {
            throw new IllegalArgumentException(
                    "Transaction must have an id");
        }

        FinancialTransaction existingTransaction = financialTransactionRepository.findById(transaction.getId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Transaction does not exist: "
                                + transaction.getId()));

        long categoryId = transaction.getCategoryId();

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Category does not exist with id: " + categoryId));

        if (category.getType() != transaction.getType()) {
            throw new IllegalArgumentException(
                    "Category type does not match transaction type");
        }

        boolean categoryChanged = existingTransaction.getCategoryId() != categoryId;

        if (categoryChanged && !category.isActive()) {
            throw new IllegalArgumentException(
                    "Cannot assign an inactive category");
        }

        return financialTransactionRepository.update(transaction);
    }

    @Override
    public boolean deleteTransaction(long transactionId) {
        if (transactionId <= 0) {
            throw new IllegalArgumentException("transactionId must be positive");
        }
        return financialTransactionRepository.deleteById(transactionId);
    }

    private static FinancialTransactionQuery prepareQuery(
            long profileId,
            Necessity necessity,
            TransactionType type,
            LocalDate startDate,
            LocalDate endDate) {
        return FinancialTransactionQuery.builder()
                .profileId(profileId)
                .necessity(necessity)
                .type(type)
                .startDate(startDate)
                .endDate(endDate)
                .build();
    }
}
