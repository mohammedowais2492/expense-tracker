package com.expensetracker.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Unit tests for {@link TransactionAggregate}. */
class TransactionAggregateTest {

    // Creation

    @Test
    void buildsSuccessfully() {
        TransactionAggregate aggregate = TransactionAggregate.builder()
                .totalAmount(15000L)
                .transactionCount(5L)
                .build();

        assertEquals(15000L, aggregate.getTotalAmount());
        assertEquals(5L, aggregate.getTransactionCount());
    }

    @Test
    void buildsSuccessfullyWhenNoTransactionsExist() {
        TransactionAggregate aggregate = TransactionAggregate.builder()
                .totalAmount(0L)
                .transactionCount(0L)
                .build();

        assertEquals(0L, aggregate.getTotalAmount());
        assertEquals(0L, aggregate.getTransactionCount());
    }

    @ParameterizedTest
    @ValueSource(longs = { -1L, -100L, Long.MIN_VALUE })
    void buildsSuccessfullyWhenTotalAmountIsNegative(long totalAmount) {
        TransactionAggregate aggregate = TransactionAggregate.builder()
                .totalAmount(totalAmount)
                .transactionCount(2L)
                .build();

        assertEquals(totalAmount, aggregate.getTotalAmount());
        assertEquals(2L, aggregate.getTransactionCount());
    }

    @Test
    void buildsSuccessfullyWhenTotalAmountIsZero() {
        TransactionAggregate aggregate = TransactionAggregate.builder()
                .totalAmount(0L)
                .transactionCount(3L)
                .build();

        assertEquals(0L, aggregate.getTotalAmount());
        assertEquals(3L, aggregate.getTransactionCount());
    }

    @Test
    void buildsSuccessfullyWithMaximumLongValues() {
        TransactionAggregate aggregate = TransactionAggregate.builder()
                .totalAmount(Long.MAX_VALUE)
                .transactionCount(Long.MAX_VALUE)
                .build();

        assertEquals(Long.MAX_VALUE, aggregate.getTotalAmount());
        assertEquals(Long.MAX_VALUE, aggregate.getTransactionCount());
    }

    @Test
    void buildsSuccessfullyWithDefaultBuilderValues() {
        TransactionAggregate aggregate = TransactionAggregate.builder()
                .build();

        assertEquals(0L, aggregate.getTotalAmount());
        assertEquals(0L, aggregate.getTransactionCount());
    }

    // Validation

    @ParameterizedTest
    @ValueSource(longs = { -1L, -100L, Long.MIN_VALUE })
    void throwsIllegalArgumentExceptionWhenTransactionCountIsNegative(long transactionCount) {
        assertThrows(
                IllegalArgumentException.class,
                () -> TransactionAggregate.builder()
                        .totalAmount(1000L)
                        .transactionCount(transactionCount)
                        .build());
    }

    // Value semantics

    @Test
    void aggregatesWithSameValuesAreEqual() {
        TransactionAggregate first = TransactionAggregate.builder()
                .totalAmount(15000L)
                .transactionCount(5L)
                .build();

        TransactionAggregate second = TransactionAggregate.builder()
                .totalAmount(15000L)
                .transactionCount(5L)
                .build();

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
    }

    @Test
    void aggregatesWithDifferentTotalAmountsAreNotEqual() {
        TransactionAggregate first = TransactionAggregate.builder()
                .totalAmount(15000L)
                .transactionCount(5L)
                .build();

        TransactionAggregate second = TransactionAggregate.builder()
                .totalAmount(20000L)
                .transactionCount(5L)
                .build();

        assertNotEquals(first, second);
    }

    @Test
    void aggregatesWithDifferentTransactionCountsAreNotEqual() {
        TransactionAggregate first = TransactionAggregate.builder()
                .totalAmount(15000L)
                .transactionCount(5L)
                .build();

        TransactionAggregate second = TransactionAggregate.builder()
                .totalAmount(15000L)
                .transactionCount(10L)
                .build();

        assertNotEquals(first, second);
    }

    @Test
    void toStringContainsAggregateValues() {
        TransactionAggregate aggregate = TransactionAggregate.builder()
                .totalAmount(15000L)
                .transactionCount(5L)
                .build();

        String representation = aggregate.toString();

        assertTrue(representation.contains("15000"));
        assertTrue(representation.contains("5"));
    }
}
