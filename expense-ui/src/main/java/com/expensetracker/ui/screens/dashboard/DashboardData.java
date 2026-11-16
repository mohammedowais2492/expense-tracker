package com.expensetracker.ui.screens.dashboard;

import java.util.List;
import java.util.Objects;

import com.expensetracker.models.FinancialTransaction;

/**
 * Contains the financial information displayed on the dashboard.
 *
 * <p>
 * Monetary amounts are represented in integer cents.
 *
 * @param thisMonth          total expenses for the current month in cents
 * @param lastMonth          total expenses for the previous month in cents
 * @param thisYear           total expenses for the current year in cents
 * @param recentTransactions the five most recent expense transactions
 */
record DashboardData(
        long thisMonth,
        long lastMonth,
        long thisYear,
        List<FinancialTransaction> recentTransactions) {

    DashboardData {
        Objects.requireNonNull(
                recentTransactions,
                "recentTransactions must not be null");

        recentTransactions = List.copyOf(recentTransactions);
    }
}