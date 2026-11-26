package com.expensetracker.ui.screens.transactions;

import java.util.Objects;

import com.expensetracker.models.FinancialTransaction;
import com.expensetracker.models.Page;
import com.expensetracker.models.PageRequest;
import com.expensetracker.models.TransactionType;
import com.expensetracker.services.FinancialTransactionService;
import com.expensetracker.ui.context.UserContext;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * Coordinates retrieval of expense transaction history for the desktop UI.
 *
 * <p>
 * Uses the currently selected expense profile from {@link UserContext}
 * to retrieve paginated expense transactions.
 *
 * <p>
 * Delegates transaction retrieval and pagination to
 * {@link FinancialTransactionService}. This service contains no JavaFX
 * dependencies and does not manage UI presentation.
 */
@RequiredArgsConstructor
public final class TransactionHistoryService {

    @NonNull
    private final UserContext userContext;

    @NonNull
    private final FinancialTransactionService financialTransactionService;

    /**
     * Retrieves a page of expense transactions for the currently
     * selected expense profile.
     *
     * <p>
     * Only transactions of type {@link TransactionType#EXPENSE}
     * are returned. Pagination is controlled by the supplied request.
     *
     * @param pageRequest pagination parameters; must not be null
     * @return a page containing expense transactions
     * @throws NullPointerException  if pageRequest is null
     * @throws IllegalStateException if no expense profile is selected
     */
    public Page<FinancialTransaction> getExpenses(PageRequest pageRequest) {
        Objects.requireNonNull(pageRequest, "pageRequest must not be null");

        Long profileId = userContext.getSelectedProfileId();

        if (profileId == null) {
            throw new IllegalStateException("No expense profile selected");
        }

        return financialTransactionService.getFinancialTransactionsOfType(
                profileId,
                TransactionType.EXPENSE,
                pageRequest);
    }
}