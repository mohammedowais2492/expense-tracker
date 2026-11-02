
package com.expensetracker.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/** Unit tests for {@link FinancialTransaction.java} */
class FinancialTransactionTest {

    private static final long PROFILE_ID = 1L;
    private static final long CATEGORY_ID = 2L;
    private static final long AMOUNT = 2599L;
    private static final LocalDate TRANSACTION_DATE = LocalDate.of(2026, 10, 31);
    private static final String DESCRIPTION = "Groceries";
    private static final long TRANSACTION_ID = 10L;

    // Builder: valid construction

    @Test
    void buildsSuccessfullyWhenAllFieldsAreSpecified() {
        FinancialTransaction transaction = createExpense(Necessity.NECESSARY);

        assertEquals(PROFILE_ID, transaction.getProfileId());
        assertEquals(CATEGORY_ID, transaction.getCategoryId());
        assertEquals(AMOUNT, transaction.getAmount());
        assertEquals(TransactionType.EXPENSE, transaction.getType());
        assertEquals(TRANSACTION_DATE, transaction.getTransactionDate());
        assertEquals(DESCRIPTION, transaction.getDescription());
        assertEquals(Necessity.NECESSARY, transaction.getNecessity());
        assertNull(transaction.getId());
    }

    @Test
    void buildsSuccessfullyWhenExpenseIsUnnecessary() {
        FinancialTransaction transaction = createExpense(Necessity.UNNECESSARY);

        assertEquals(Necessity.UNNECESSARY, transaction.getNecessity());
    }

    @Test
    void buildsSuccessfullyWhenTransactionIsIncome() {
        FinancialTransaction transaction = createIncome();

        assertEquals(TransactionType.INCOME, transaction.getType());
        assertNull(transaction.getNecessity());
    }

    // Builder: required fields

    @Test
    void throwsNullPointerExceptionWhenTypeIsNull() {
        assertThrows(NullPointerException.class, () -> FinancialTransaction.builder()
                .profileId(PROFILE_ID)
                .categoryId(CATEGORY_ID)
                .amount(AMOUNT)
                .type(null)
                .transactionDate(TRANSACTION_DATE)
                .build());
    }

    @Test
    void throwsNullPointerExceptionWhenDateIsNull() {
        assertThrows(NullPointerException.class, () -> FinancialTransaction.builder()
                .profileId(PROFILE_ID)
                .categoryId(CATEGORY_ID)
                .amount(AMOUNT)
                .type(TransactionType.EXPENSE)
                .transactionDate(null)
                .necessity(Necessity.NECESSARY)
                .build());
    }

    // Builder: numeric validation

    @ParameterizedTest
    @ValueSource(longs = { 0L, -1L })
    void throwsIllegalArgumentExceptionWhenProfileIdIsNonPositive(long profileId) {
        assertThrows(IllegalArgumentException.class, () -> FinancialTransaction.builder()
                .profileId(profileId)
                .categoryId(CATEGORY_ID)
                .amount(AMOUNT)
                .type(TransactionType.EXPENSE)
                .transactionDate(TRANSACTION_DATE)
                .necessity(Necessity.NECESSARY)
                .build());
    }

    @ParameterizedTest
    @ValueSource(longs = { 0L, -1L })
    void throwsIllegalArgumentExceptionWhenCategoryIdIsNonPositive(long categoryId) {
        assertThrows(IllegalArgumentException.class, () -> FinancialTransaction.builder()
                .profileId(PROFILE_ID)
                .categoryId(categoryId)
                .amount(AMOUNT)
                .type(TransactionType.EXPENSE)
                .transactionDate(TRANSACTION_DATE)
                .necessity(Necessity.NECESSARY)
                .build());
    }

    @ParameterizedTest
    @ValueSource(longs = { 0L, -1L })
    void throwsIllegalArgumentExceptionWhenAmountIsNonPositive(long amount) {
        assertThrows(IllegalArgumentException.class, () -> FinancialTransaction.builder()
                .profileId(PROFILE_ID)
                .categoryId(CATEGORY_ID)
                .amount(amount)
                .type(TransactionType.EXPENSE)
                .transactionDate(TRANSACTION_DATE)
                .necessity(Necessity.NECESSARY)
                .build());
    }

    // Builder: description handling

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { " ", "   ", "\t", "\n" })
    void buildsSuccessfullyWithNullOrBlankDescription(String description) {
        FinancialTransaction transaction = FinancialTransaction.builder()
                .profileId(PROFILE_ID)
                .categoryId(CATEGORY_ID)
                .amount(AMOUNT)
                .type(TransactionType.EXPENSE)
                .transactionDate(TRANSACTION_DATE)
                .description(description)
                .necessity(Necessity.NECESSARY)
                .build();

        assertNull(transaction.getDescription());
    }

    @Test
    void preservesNonBlankDescription() {
        FinancialTransaction transaction = createExpense(Necessity.NECESSARY);

        assertEquals(DESCRIPTION, transaction.getDescription());
    }

    // Builder: business invariants

    @Test
    void throwsIllegalArgumentExceptionWhenIncomeHasNecessity() {
        assertThrows(IllegalArgumentException.class, () -> FinancialTransaction.builder()
                .profileId(PROFILE_ID)
                .categoryId(CATEGORY_ID)
                .amount(AMOUNT)
                .type(TransactionType.INCOME)
                .transactionDate(TRANSACTION_DATE)
                .necessity(Necessity.NECESSARY)
                .build());
    }

    @Test
    void throwsIllegalArgumentExceptionWhenExpenseHasNoNecessity() {
        assertThrows(IllegalArgumentException.class, () -> FinancialTransaction.builder()
                .profileId(PROFILE_ID)
                .categoryId(CATEGORY_ID)
                .amount(AMOUNT)
                .type(TransactionType.EXPENSE)
                .transactionDate(TRANSACTION_DATE)
                .build());
    }

    // Reconstitution: valid construction

    @Test
    void reconstituteConstructsSuccessfullyWhenAllFieldsAreSpecified() {
        FinancialTransaction transaction = createReconstitutedExpense(Necessity.NECESSARY);

        assertEquals(TRANSACTION_ID, transaction.getId());
        assertEquals(PROFILE_ID, transaction.getProfileId());
        assertEquals(CATEGORY_ID, transaction.getCategoryId());
        assertEquals(AMOUNT, transaction.getAmount());
        assertEquals(TransactionType.EXPENSE, transaction.getType());
        assertEquals(TRANSACTION_DATE, transaction.getTransactionDate());
        assertEquals(DESCRIPTION, transaction.getDescription());
        assertEquals(Necessity.NECESSARY, transaction.getNecessity());
    }

    @Test
    void reconstituteConstructsSuccessfullyWhenTransactionIsIncome() {
        FinancialTransaction transaction = FinancialTransaction.reconstitute(
                TRANSACTION_ID,
                PROFILE_ID,
                CATEGORY_ID,
                AMOUNT,
                TransactionType.INCOME,
                TRANSACTION_DATE,
                DESCRIPTION,
                null);

        assertEquals(TRANSACTION_ID, transaction.getId());
        assertEquals(TransactionType.INCOME, transaction.getType());
        assertNull(transaction.getNecessity());
    }

    @Test
    void reconstituteConstructsSuccessfullyWhenExpenseIsUnnecessary() {
        FinancialTransaction transaction = createReconstitutedExpense(Necessity.UNNECESSARY);

        assertEquals(Necessity.UNNECESSARY, transaction.getNecessity());
    }

    // Reconstitution: required fields

    @Test
    void reconstituteThrowsNullPointerExceptionWhenTypeIsNull() {
        assertThrows(NullPointerException.class, () -> FinancialTransaction.reconstitute(
                TRANSACTION_ID,
                PROFILE_ID,
                CATEGORY_ID,
                AMOUNT,
                null,
                TRANSACTION_DATE,
                DESCRIPTION,
                Necessity.NECESSARY));
    }

    @Test
    void reconstituteThrowsNullPointerExceptionWhenDateIsNull() {
        assertThrows(NullPointerException.class, () -> FinancialTransaction.reconstitute(
                TRANSACTION_ID,
                PROFILE_ID,
                CATEGORY_ID,
                AMOUNT,
                TransactionType.EXPENSE,
                null,
                DESCRIPTION,
                Necessity.NECESSARY));
    }

    // Reconstitution: numeric validation

    @ParameterizedTest
    @ValueSource(longs = { 0L, -1L })
    void reconstituteThrowsIllegalArgumentExceptionWhenIdIsNonPositive(long id) {
        assertThrows(IllegalArgumentException.class, () -> FinancialTransaction.reconstitute(
                id,
                PROFILE_ID,
                CATEGORY_ID,
                AMOUNT,
                TransactionType.EXPENSE,
                TRANSACTION_DATE,
                DESCRIPTION,
                Necessity.NECESSARY));
    }

    @ParameterizedTest
    @ValueSource(longs = { 0L, -1L })
    void reconstituteThrowsIllegalArgumentExceptionWhenProfileIdIsNonPositive(long profileId) {
        assertThrows(IllegalArgumentException.class, () -> FinancialTransaction.reconstitute(
                TRANSACTION_ID,
                profileId,
                CATEGORY_ID,
                AMOUNT,
                TransactionType.EXPENSE,
                TRANSACTION_DATE,
                DESCRIPTION,
                Necessity.NECESSARY));
    }

    @ParameterizedTest
    @ValueSource(longs = { 0L, -1L })
    void reconstituteThrowsIllegalArgumentExceptionWhenCategoryIdIsNonPositive(long categoryId) {
        assertThrows(IllegalArgumentException.class, () -> FinancialTransaction.reconstitute(
                TRANSACTION_ID,
                PROFILE_ID,
                categoryId,
                AMOUNT,
                TransactionType.EXPENSE,
                TRANSACTION_DATE,
                DESCRIPTION,
                Necessity.NECESSARY));
    }

    @ParameterizedTest
    @ValueSource(longs = { 0L, -1L })
    void reconstituteThrowsIllegalArgumentExceptionWhenAmountIsNonPositive(long amount) {
        assertThrows(IllegalArgumentException.class, () -> FinancialTransaction.reconstitute(
                TRANSACTION_ID,
                PROFILE_ID,
                CATEGORY_ID,
                amount,
                TransactionType.EXPENSE,
                TRANSACTION_DATE,
                DESCRIPTION,
                Necessity.NECESSARY));
    }

    // Reconstitution: description handling

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { " ", "   ", "\t", "\n" })
    void reconstituteConstructsSuccessfullyWithNullOrBlankDescription(String description) {
        FinancialTransaction transaction = FinancialTransaction.reconstitute(
                TRANSACTION_ID,
                PROFILE_ID,
                CATEGORY_ID,
                AMOUNT,
                TransactionType.EXPENSE,
                TRANSACTION_DATE,
                description,
                Necessity.NECESSARY);

        assertNull(transaction.getDescription());
        assertEquals(TRANSACTION_ID, transaction.getId());
    }

    @Test
    void reconstitutePreservesNonBlankDescription() {
        FinancialTransaction transaction = createReconstitutedExpense(Necessity.NECESSARY);

        assertEquals(DESCRIPTION, transaction.getDescription());
    }

    // Reconstitution: business invariants

    @Test
    void reconstituteThrowsIllegalArgumentExceptionWhenIncomeHasNecessity() {
        assertThrows(IllegalArgumentException.class, () -> FinancialTransaction.reconstitute(
                TRANSACTION_ID,
                PROFILE_ID,
                CATEGORY_ID,
                AMOUNT,
                TransactionType.INCOME,
                TRANSACTION_DATE,
                DESCRIPTION,
                Necessity.NECESSARY));
    }

    @Test
    void reconstituteThrowsIllegalArgumentExceptionWhenExpenseHasNoNecessity() {
        assertThrows(IllegalArgumentException.class, () -> FinancialTransaction.reconstitute(
                TRANSACTION_ID,
                PROFILE_ID,
                CATEGORY_ID,
                AMOUNT,
                TransactionType.EXPENSE,
                TRANSACTION_DATE,
                DESCRIPTION,
                null));
    }

    // Test helpers

    private FinancialTransaction createExpense(Necessity necessity) {
        return FinancialTransaction.builder()
                .profileId(PROFILE_ID)
                .categoryId(CATEGORY_ID)
                .amount(AMOUNT)
                .type(TransactionType.EXPENSE)
                .transactionDate(TRANSACTION_DATE)
                .description(DESCRIPTION)
                .necessity(necessity)
                .build();
    }

    private FinancialTransaction createIncome() {
        return FinancialTransaction.builder()
                .profileId(PROFILE_ID)
                .categoryId(CATEGORY_ID)
                .amount(AMOUNT)
                .type(TransactionType.INCOME)
                .transactionDate(TRANSACTION_DATE)
                .description(DESCRIPTION)
                .build();
    }

    private FinancialTransaction createReconstitutedExpense(Necessity necessity) {
        return FinancialTransaction.reconstitute(
                TRANSACTION_ID,
                PROFILE_ID,
                CATEGORY_ID,
                AMOUNT,
                TransactionType.EXPENSE,
                TRANSACTION_DATE,
                DESCRIPTION,
                necessity);
    }
}
