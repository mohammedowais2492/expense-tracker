package com.expensetracker.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Unit tests for {@link Category} */
class CategoryTest {

    // Creation

    @Test
    void buildsSuccessfullyAndIsActiveByDefault() {
        Category category = Category.builder()
                .name("Food")
                .type(TransactionType.EXPENSE)
                .build();

        assertEquals("Food", category.getName());
        assertEquals(TransactionType.EXPENSE, category.getType());
        assertTrue(category.isActive());
    }

    @Test
    void throwsNullPointerExceptionWhenNameIsNull() {
        assertThrows(
                NullPointerException.class,
                () -> Category.builder()
                        .name(null)
                        .type(TransactionType.EXPENSE)
                        .build());
    }

    @ParameterizedTest
    @ValueSource(strings = { "", " ", "   ", "\t", "\n" })
    void throwsIllegalArgumentExceptionWhenNameIsBlank(String name) {
        assertThrows(
                IllegalArgumentException.class,
                () -> Category.builder()
                        .name(name)
                        .type(TransactionType.EXPENSE)
                        .build());
    }

    @Test
    void buildsSuccessfullyAndTrimsName() {
        Category category = Category.builder()
                .name("  Food  ")
                .type(TransactionType.EXPENSE)
                .build();

        assertEquals("Food", category.getName());
    }

    @Test
    void throwsNullPointerExceptionWhenTypeIsNull() {
        assertThrows(
                NullPointerException.class,
                () -> Category.builder()
                        .name("Food")
                        .type(null)
                        .build());
    }

    // Reconstitution

    @Test
    void reconstituteConstructsSuccessfully() {
        Category category = Category.reconstitute(
                1L, "Food", TransactionType.EXPENSE, false);

        assertEquals(1L, category.getId());
        assertEquals("Food", category.getName());
        assertEquals(TransactionType.EXPENSE, category.getType());
        assertFalse(category.isActive());
    }

    @ParameterizedTest
    @ValueSource(longs = { 0L, -1L })
    void reconstituteThrowsIllegalArgumentExceptionWhenIdIsNonPositive(long id) {
        assertThrows(
                IllegalArgumentException.class,
                () -> Category.reconstitute(
                        id, "Food", TransactionType.EXPENSE, true));
    }

    @Test
    void reconstituteThrowsNullPointerExceptionWhenNameIsNull() {
        assertThrows(
                NullPointerException.class,
                () -> Category.reconstitute(
                        1L, null, TransactionType.EXPENSE, true));
    }

    @ParameterizedTest
    @ValueSource(strings = { "", " ", "   ", "\t", "\n" })
    void reconstituteThrowsIllegalArgumentExceptionWhenNameIsBlank(String name) {
        assertThrows(
                IllegalArgumentException.class,
                () -> Category.reconstitute(
                        1L, name, TransactionType.EXPENSE, true));
    }

    @Test
    void reconstituteThrowsNullPointerExceptionWhenTypeIsNull() {
        assertThrows(
                NullPointerException.class,
                () -> Category.reconstitute(
                        1L, "Food", null, true));
    }

    @Test
    void reconstituteConstructsSuccessfullyAndTrimsName() {
        Category category = Category.reconstitute(
                1L, "  Food  ", TransactionType.EXPENSE, true);

        assertEquals("Food", category.getName());
    }

    // Renaming

    @Test
    void renameIsSuccessful() {
        Category original = Category.reconstitute(
                1L, "Food", TransactionType.EXPENSE, true);

        Category renamed = original.rename("Groceries");

        assertNotSame(original, renamed);
        assertEquals(1L, renamed.getId());
        assertEquals("Groceries", renamed.getName());
        assertEquals(TransactionType.EXPENSE, renamed.getType());
        assertTrue(renamed.isActive());

        assertEquals("Food", original.getName());
    }

    @Test
    void renameIsSuccessfulAndTrimsName() {
        Category category = Category.reconstitute(
                1L, "Food", TransactionType.EXPENSE, true);

        Category renamed = category.rename("  Groceries  ");

        assertEquals("Groceries", renamed.getName());
    }

    @Test
    void renameThrowsNullPointerExceptionWhenNameIsNull() {
        Category category = Category.reconstitute(
                1L, "Food", TransactionType.EXPENSE, true);

        assertThrows(
                NullPointerException.class,
                () -> category.rename(null));
    }

    @ParameterizedTest
    @ValueSource(strings = { "", " ", "   ", "\t", "\n" })
    void renameThrowsIllegalArgumentExceptionWhenNameIsBlank(String name) {
        Category category = Category.reconstitute(
                1L, "Food", TransactionType.EXPENSE, true);

        assertThrows(
                IllegalArgumentException.class,
                () -> category.rename(name));
    }

    // Activation

    @Test
    void activateSucceeds() {
        Category original = Category.reconstitute(
                1L, "Food", TransactionType.EXPENSE, false);

        Category activated = original.activate();

        assertNotSame(original, activated);
        assertEquals(1L, activated.getId());
        assertEquals("Food", activated.getName());
        assertEquals(TransactionType.EXPENSE, activated.getType());
        assertTrue(activated.isActive());

        assertFalse(original.isActive());
    }

    @Test
    void activateIsIdempotent() {
        Category original = Category.reconstitute(
                1L, "Food", TransactionType.EXPENSE, true);

        Category activated = original.activate();

        assertTrue(activated.isActive());
        assertNotSame(original, activated);
    }

    // Deactivation

    @Test
    void deactivateSucceeds() {
        Category original = Category.reconstitute(
                1L, "Food", TransactionType.EXPENSE, true);

        Category deactivated = original.deactivate();

        assertNotSame(original, deactivated);
        assertEquals(1L, deactivated.getId());
        assertEquals("Food", deactivated.getName());
        assertEquals(TransactionType.EXPENSE, deactivated.getType());
        assertFalse(deactivated.isActive());

        assertTrue(original.isActive());
    }

    @Test
    void deactivateIsIdempotent() {
        Category original = Category.reconstitute(
                1L, "Food", TransactionType.EXPENSE, false);

        Category deactivated = original.deactivate();

        assertFalse(deactivated.isActive());
        assertNotSame(original, deactivated);
    }
}