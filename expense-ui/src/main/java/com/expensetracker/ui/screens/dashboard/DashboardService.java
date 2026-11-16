package com.expensetracker.ui.screens.dashboard;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import com.expensetracker.models.FinancialTransaction;
import com.expensetracker.models.PageRequest;
import com.expensetracker.models.TransactionType;
import com.expensetracker.services.FinancialTransactionService;
import com.expensetracker.ui.context.UserContext;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * Coordinates retrieval of expense data displayed on the dashboard.
 *
 * <p>
 * Calculates reporting periods using the supplied clock and retrieves
 * expense totals and recent transactions through the backend service.
 *
 * <p>
 * All monetary totals are expressed in integer cents.
 */
@RequiredArgsConstructor
public class DashboardService {
    @NonNull
    private final UserContext userContext;
    @NonNull
    private final FinancialTransactionService financialTransactionService;
    @NonNull
    private final Clock clock;

    /**
     * Retrieves the expense totals and recent transactions for the
     * currently selected expense profile.
     *
     * <p>
     * Includes current-month and year-to-date expenses through today,
     * the complete previous month's expenses, and up to five recent
     * expense transactions.
     *
     * @return the dashboard data for the selected profile
     * @throws IllegalStateException if no expense profile is selected
     */
    public DashboardData getDashboardData() {
        Long profileId = userContext.getSelectedProfileId();

        if (profileId == null) {
            throw new IllegalStateException("No expense profile selected");
        }

        LocalDate date = LocalDate.now(clock);
        return new DashboardData(
                getTotalForCurrentMonth(profileId, date),
                getTotalForPreviousMonth(profileId, date),
                getTotalForYear(profileId, date),
                getRecentTransactions(profileId));
    }

    private long getTotalForCurrentMonth(long profileId, LocalDate date) {
        return financialTransactionService.getTotalExpenses(
                profileId,
                YearMonth.from(date).atDay(1),
                date.plusDays(1));
    }

    private long getTotalForPreviousMonth(long profileId, LocalDate date) {
        YearMonth previousMonth = YearMonth.from(date).minusMonths(1);

        return financialTransactionService.getTotalExpenses(
                profileId,
                previousMonth.atDay(1),
                previousMonth.plusMonths(1).atDay(1));
    }

    private long getTotalForYear(long profileId, LocalDate date) {
        return financialTransactionService.getTotalExpenses(
                profileId,
                date.withDayOfYear(1),
                date.plusDays(1));
    }

    private List<FinancialTransaction> getRecentTransactions(long profileId) {
        PageRequest pageRequest = PageRequest.builder().pageSize(5).build();
        return financialTransactionService.getFinancialTransactionsOfType(
                profileId, TransactionType.EXPENSE, pageRequest).getContent();
    }
}
