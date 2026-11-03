
package com.expensetracker.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** Unit tests for {@link FinancialTransactionQuery} */
class FinancialTransactionQueryTest {

    private static final long PROFILE_ID = 1L;
    private static final long CATEGORY_ID = 2L;
    private static final LocalDate START_DATE = LocalDate.of(2026, 10, 1);
    private static final LocalDate END_DATE = LocalDate.of(2026, 10, 31);

    @Test
    void successfullyBuildsWithAllCriteria() {
        FinancialTransactionQuery query = FinancialTransactionQuery.builder()
                .profileId(PROFILE_ID)
                .categoryId(CATEGORY_ID)
                .necessity(Necessity.NECESSARY)
                .type(TransactionType.EXPENSE)
                .startDate(START_DATE)
                .endDate(END_DATE)
                .build();

        assertEquals(PROFILE_ID, query.getProfileId());
        assertEquals(CATEGORY_ID, query.getCategoryId());
        assertEquals(Necessity.NECESSARY, query.getNecessity());
        assertEquals(TransactionType.EXPENSE, query.getType());
        assertEquals(START_DATE, query.getStartDate());
        assertEquals(END_DATE, query.getEndDate());
    }

    @ParameterizedTest
    @MethodSource("individualCriteria")
    void successfullyBuildsWithIndividualCriterion(
            Long categoryId,
            Necessity necessity,
            LocalDate startDate,
            LocalDate endDate) {

        FinancialTransactionQuery query = FinancialTransactionQuery.builder()
                .profileId(PROFILE_ID)
                .categoryId(categoryId)
                .necessity(necessity)
                .type(TransactionType.EXPENSE)
                .startDate(startDate)
                .endDate(endDate)
                .build();

        assertEquals(PROFILE_ID, query.getProfileId());
        assertEquals(categoryId, query.getCategoryId());
        assertEquals(necessity, query.getNecessity());
        assertEquals(TransactionType.EXPENSE, query.getType());
        assertEquals(startDate, query.getStartDate());
        assertEquals(endDate, query.getEndDate());
    }

    private static Stream<Arguments> individualCriteria() {
        return Stream.of(
                Arguments.of(CATEGORY_ID, null, null, null),
                Arguments.of(null, Necessity.NECESSARY, null, null),
                Arguments.of(null, null, START_DATE, null),
                Arguments.of(null, null, null, END_DATE));
    }

    @Test
    void successfullyBuildsWithJustStartDate() {
        FinancialTransactionQuery query = FinancialTransactionQuery.builder()
                .profileId(PROFILE_ID)
                .type(TransactionType.EXPENSE)
                .startDate(START_DATE)
                .build();

        assertEquals(START_DATE, query.getStartDate());
        assertNull(query.getEndDate());
    }

    @Test
    void successfullyBuildsWithJustEndDate() {
        FinancialTransactionQuery query = FinancialTransactionQuery.builder()
                .profileId(PROFILE_ID)
                .type(TransactionType.EXPENSE)
                .endDate(END_DATE)
                .build();

        assertNull(query.getStartDate());
        assertEquals(END_DATE, query.getEndDate());
    }

    @Test
    void successfullyBuildsWithNoOptionalCriteria() {
        FinancialTransactionQuery query = FinancialTransactionQuery.builder()
                .profileId(PROFILE_ID)
                .type(TransactionType.EXPENSE)
                .build();

        assertEquals(PROFILE_ID, query.getProfileId());
        assertEquals(TransactionType.EXPENSE, query.getType());
        assertNull(query.getCategoryId());
        assertNull(query.getNecessity());
        assertNull(query.getStartDate());
        assertNull(query.getEndDate());
    }

    @Test
    void successfullyBuildsWithEqualStartDateAndEndDate() {
        FinancialTransactionQuery query = FinancialTransactionQuery.builder()
                .profileId(PROFILE_ID)
                .type(TransactionType.EXPENSE)
                .startDate(START_DATE)
                .endDate(START_DATE)
                .build();

        assertEquals(START_DATE, query.getStartDate());
        assertEquals(START_DATE, query.getEndDate());
    }

    @Test
    void throwsNullPointerExceptionWhenTypeIsNull() {
        assertThrows(NullPointerException.class, () -> FinancialTransactionQuery.builder()
                .profileId(PROFILE_ID)
                .build());
    }

    @ParameterizedTest
    @MethodSource("nonPositiveIds")
    void throwsIllegalArgumentExceptionWhenProfileIdIsNotPositive(long profileId) {
        assertThrows(IllegalArgumentException.class, () -> FinancialTransactionQuery.builder()
                .profileId(profileId)
                .type(TransactionType.EXPENSE)
                .build());
    }

    @ParameterizedTest
    @MethodSource("nonPositiveIds")
    void throwsIllegalArgumentExceptionWhenCategoryIdIsNotPositive(long categoryId) {
        assertThrows(IllegalArgumentException.class, () -> FinancialTransactionQuery.builder()
                .profileId(PROFILE_ID)
                .categoryId(categoryId)
                .type(TransactionType.EXPENSE)
                .build());
    }

    private static Stream<Long> nonPositiveIds() {
        return Stream.of(0L, -1L);
    }

    @Test
    void throwsIllegalArgumentExceptionWhenTypeIsIncomeAndNecessityIsNotNull() {
        assertThrows(IllegalArgumentException.class, () -> FinancialTransactionQuery.builder()
                .profileId(PROFILE_ID)
                .necessity(Necessity.NECESSARY)
                .type(TransactionType.INCOME)
                .build());
    }

    @Test
    void throwsIllegalArgumentExceptionWhenStartDateIsAfterEndDate() {
        assertThrows(IllegalArgumentException.class, () -> FinancialTransactionQuery.builder()
                .profileId(PROFILE_ID)
                .type(TransactionType.EXPENSE)
                .startDate(END_DATE)
                .endDate(START_DATE)
                .build());
    }
}
