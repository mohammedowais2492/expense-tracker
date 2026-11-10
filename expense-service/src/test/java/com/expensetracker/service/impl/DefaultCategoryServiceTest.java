package com.expensetracker.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.expensetracker.models.Category;
import com.expensetracker.models.TransactionType;
import com.expensetracker.repositories.CategoryRepository;

/**
 * Unit tests for {@link DefaultCategoryService}.
 *
 * <p>
 * Uses a mocked {@link CategoryRepository} to verify input
 * validation, repository delegation, category type immutability,
 * return values, and exception propagation without accessing
 * a database.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DefaultCategoryService Tests")
class DefaultCategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    private DefaultCategoryService categoryService;

    @BeforeEach
    void setUp() {
        categoryService = new DefaultCategoryService(categoryRepository);
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Creates service with valid repository")
        void createsServiceWithValidRepository() {
            DefaultCategoryService service = new DefaultCategoryService(categoryRepository);

            assertNotNull(service);
        }

        @Test
        @DisplayName("Throws NullPointerException when repository is null")
        void throwsNullPointerExceptionWhenRepositoryIsNull() {
            NullPointerException exception = assertThrows(
                    NullPointerException.class,
                    () -> new DefaultCategoryService(null));

            assertEquals(
                    "categoryRepository must not be null",
                    exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Create Category Tests")
    class CreateCategoryTests {

        @Test
        @DisplayName("Creates category and returns persisted category")
        void createsCategoryAndReturnsPersistedCategory() {
            Category newCategory = Category.builder()
                    .name("Groceries")
                    .type(TransactionType.EXPENSE)
                    .build();

            Category persistedCategory = Category.reconstitute(
                    1L, "Groceries", TransactionType.EXPENSE, true);

            when(categoryRepository.save(newCategory))
                    .thenReturn(persistedCategory);

            Category result = categoryService.createCategory(newCategory);

            assertSame(persistedCategory, result);
            verify(categoryRepository).save(newCategory);
        }

        @Test
        @DisplayName("Creates income category")
        void createsIncomeCategory() {
            Category newCategory = Category.builder()
                    .name("Salary")
                    .type(TransactionType.INCOME)
                    .build();

            Category persistedCategory = Category.reconstitute(
                    2L, "Salary", TransactionType.INCOME, true);

            when(categoryRepository.save(newCategory))
                    .thenReturn(persistedCategory);

            Category result = categoryService.createCategory(newCategory);

            assertSame(persistedCategory, result);
            verify(categoryRepository).save(newCategory);
        }

        @Test
        @DisplayName("Throws NullPointerException when category is null")
        void throwsNullPointerExceptionWhenCategoryIsNull() {
            NullPointerException exception = assertThrows(
                    NullPointerException.class,
                    () -> categoryService.createCategory(null));

            assertEquals(
                    "category must not be null",
                    exception.getMessage());

            verifyNoInteractions(categoryRepository);
        }

        @Test
        @DisplayName("Throws IllegalArgumentException when category already has an ID")
        void throwsIllegalArgumentExceptionWhenCategoryAlreadyHasId() {
            Category existingCategory = Category.reconstitute(
                    1L, "Groceries", TransactionType.EXPENSE, true);

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> categoryService.createCategory(existingCategory));

            assertEquals(
                    "New category must not have an id",
                    exception.getMessage());

            verifyNoInteractions(categoryRepository);
        }

        @Test
        @DisplayName("Propagates repository exception when creation fails")
        void propagatesRepositoryExceptionWhenCreationFails() {
            Category newCategory = Category.builder()
                    .name("Groceries")
                    .type(TransactionType.EXPENSE)
                    .build();

            RuntimeException repositoryException = new RuntimeException("Repository failure");

            when(categoryRepository.save(newCategory))
                    .thenThrow(repositoryException);

            RuntimeException exception = assertThrows(
                    RuntimeException.class,
                    () -> categoryService.createCategory(newCategory));

            assertSame(repositoryException, exception);
            verify(categoryRepository).save(newCategory);
        }
    }

    @Nested
    @DisplayName("Update Category Tests")
    class UpdateCategoryTests {

        @Test
        @DisplayName("Updates category when transaction type is unchanged")
        void updatesCategoryWhenTransactionTypeIsUnchanged() {
            Category existingCategory = Category.reconstitute(
                    1L, "Groceries", TransactionType.EXPENSE, true);

            Category updatedCategory = Category.reconstitute(
                    1L, "Food", TransactionType.EXPENSE, true);

            when(categoryRepository.findById(1L))
                    .thenReturn(Optional.of(existingCategory));

            when(categoryRepository.update(updatedCategory))
                    .thenReturn(updatedCategory);

            Category result = categoryService.updateCategory(updatedCategory);

            assertSame(updatedCategory, result);

            verify(categoryRepository).findById(1L);
            verify(categoryRepository).update(updatedCategory);
        }

        @Test
        @DisplayName("Allows renaming a category")
        void allowsRenamingCategory() {
            Category existingCategory = Category.reconstitute(
                    1L, "Groceries", TransactionType.EXPENSE, true);

            Category renamedCategory = existingCategory.rename("Food");

            when(categoryRepository.findById(1L))
                    .thenReturn(Optional.of(existingCategory));

            when(categoryRepository.update(renamedCategory))
                    .thenReturn(renamedCategory);

            Category result = categoryService.updateCategory(renamedCategory);

            assertEquals("Food", result.getName());
            assertEquals(TransactionType.EXPENSE, result.getType());

            verify(categoryRepository).findById(1L);
            verify(categoryRepository).update(renamedCategory);
        }

        @Test
        @DisplayName("Allows deactivating a category")
        void allowsDeactivatingCategory() {
            Category existingCategory = Category.reconstitute(
                    1L, "Groceries", TransactionType.EXPENSE, true);

            Category deactivatedCategory = existingCategory.deactivate();

            when(categoryRepository.findById(1L))
                    .thenReturn(Optional.of(existingCategory));

            when(categoryRepository.update(deactivatedCategory))
                    .thenReturn(deactivatedCategory);

            Category result = categoryService.updateCategory(
                    deactivatedCategory);

            assertFalse(result.isActive());

            verify(categoryRepository).findById(1L);
            verify(categoryRepository).update(deactivatedCategory);
        }

        @Test
        @DisplayName("Allows activating an inactive category")
        void allowsActivatingInactiveCategory() {
            Category existingCategory = Category.reconstitute(
                    1L, "Groceries", TransactionType.EXPENSE, false);

            Category activatedCategory = existingCategory.activate();

            when(categoryRepository.findById(1L))
                    .thenReturn(Optional.of(existingCategory));

            when(categoryRepository.update(activatedCategory))
                    .thenReturn(activatedCategory);

            Category result = categoryService.updateCategory(
                    activatedCategory);

            assertTrue(result.isActive());

            verify(categoryRepository).findById(1L);
            verify(categoryRepository).update(activatedCategory);
        }

        @Test
        @DisplayName("Throws NullPointerException when category is null")
        void throwsNullPointerExceptionWhenCategoryIsNull() {
            NullPointerException exception = assertThrows(
                    NullPointerException.class,
                    () -> categoryService.updateCategory(null));

            assertEquals(
                    "category must not be null",
                    exception.getMessage());

            verifyNoInteractions(categoryRepository);
        }

        @Test
        @DisplayName("Throws IllegalArgumentException when category has no ID")
        void throwsIllegalArgumentExceptionWhenCategoryHasNoId() {
            Category newCategory = Category.builder()
                    .name("Groceries")
                    .type(TransactionType.EXPENSE)
                    .build();

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> categoryService.updateCategory(newCategory));

            assertEquals(
                    "categoryId must not be null",
                    exception.getMessage());

            verifyNoInteractions(categoryRepository);
        }

        @Test
        @DisplayName("Throws IllegalArgumentException when category does not exist")
        void throwsIllegalArgumentExceptionWhenCategoryDoesNotExist() {
            Category category = Category.reconstitute(
                    99L, "Groceries", TransactionType.EXPENSE, true);

            when(categoryRepository.findById(99L))
                    .thenReturn(Optional.empty());

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> categoryService.updateCategory(category));

            assertEquals(
                    "Category does not exist: 99",
                    exception.getMessage());

            verify(categoryRepository).findById(99L);
            verifyNoMoreInteractions(categoryRepository);
        }

        @Test
        @DisplayName("Throws IllegalArgumentException when transaction type changes")
        void throwsIllegalArgumentExceptionWhenTransactionTypeChanges() {
            Category existingCategory = Category.reconstitute(
                    1L, "Groceries", TransactionType.EXPENSE, true);

            Category updatedCategory = Category.reconstitute(
                    1L, "Groceries", TransactionType.INCOME, true);

            when(categoryRepository.findById(1L))
                    .thenReturn(Optional.of(existingCategory));

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> categoryService.updateCategory(updatedCategory));

            assertEquals(
                    "Category transaction type cannot be changed",
                    exception.getMessage());

            verify(categoryRepository).findById(1L);
            verifyNoMoreInteractions(categoryRepository);
        }

        @Test
        @DisplayName("Propagates repository exception when lookup fails")
        void propagatesRepositoryExceptionWhenLookupFails() {
            Category category = Category.reconstitute(
                    1L, "Groceries", TransactionType.EXPENSE, true);

            RuntimeException repositoryException = new RuntimeException("Repository lookup failure");

            when(categoryRepository.findById(1L))
                    .thenThrow(repositoryException);

            RuntimeException exception = assertThrows(
                    RuntimeException.class,
                    () -> categoryService.updateCategory(category));

            assertSame(repositoryException, exception);

            verify(categoryRepository).findById(1L);
            verifyNoMoreInteractions(categoryRepository);
        }

        @Test
        @DisplayName("Propagates repository exception when update fails")
        void propagatesRepositoryExceptionWhenUpdateFails() {
            Category existingCategory = Category.reconstitute(
                    1L, "Groceries", TransactionType.EXPENSE, true);

            Category updatedCategory = existingCategory.rename("Food");

            RuntimeException repositoryException = new RuntimeException("Repository update failure");

            when(categoryRepository.findById(1L))
                    .thenReturn(Optional.of(existingCategory));

            when(categoryRepository.update(updatedCategory))
                    .thenThrow(repositoryException);

            RuntimeException exception = assertThrows(
                    RuntimeException.class,
                    () -> categoryService.updateCategory(updatedCategory));

            assertSame(repositoryException, exception);

            verify(categoryRepository).findById(1L);
            verify(categoryRepository).update(updatedCategory);
        }
    }

    @Nested
    @DisplayName("Get Category By ID Tests")
    class GetCategoryByIdTests {

        @Test
        @DisplayName("Returns category when ID exists")
        void returnsCategoryWhenIdExists() {
            Category category = Category.reconstitute(
                    1L, "Groceries", TransactionType.EXPENSE, true);

            when(categoryRepository.findById(1L))
                    .thenReturn(Optional.of(category));

            Optional<Category> result = categoryService.getCategoryById(1L);

            assertTrue(result.isPresent());
            assertSame(category, result.get());

            verify(categoryRepository).findById(1L);
        }

        @Test
        @DisplayName("Returns inactive category when ID exists")
        void returnsInactiveCategoryWhenIdExists() {
            Category category = Category.reconstitute(
                    1L, "Groceries", TransactionType.EXPENSE, false);

            when(categoryRepository.findById(1L))
                    .thenReturn(Optional.of(category));

            Optional<Category> result = categoryService.getCategoryById(1L);

            assertTrue(result.isPresent());
            assertFalse(result.get().isActive());

            verify(categoryRepository).findById(1L);
        }

        @Test
        @DisplayName("Returns empty Optional when category does not exist")
        void returnsEmptyOptionalWhenCategoryDoesNotExist() {
            when(categoryRepository.findById(99L))
                    .thenReturn(Optional.empty());

            Optional<Category> result = categoryService.getCategoryById(99L);

            assertTrue(result.isEmpty());
            verify(categoryRepository).findById(99L);
        }

        @Test
        @DisplayName("Throws IllegalArgumentException when ID is zero")
        void throwsIllegalArgumentExceptionWhenIdIsZero() {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> categoryService.getCategoryById(0L));

            verifyNoInteractions(categoryRepository);
        }

        @Test
        @DisplayName("Throws IllegalArgumentException when ID is negative")
        void throwsIllegalArgumentExceptionWhenIdIsNegative() {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> categoryService.getCategoryById(-1L));

            verifyNoInteractions(categoryRepository);
        }

        @Test
        @DisplayName("Propagates repository exception when lookup fails")
        void propagatesRepositoryExceptionWhenLookupFails() {
            RuntimeException repositoryException = new RuntimeException("Repository failure");

            when(categoryRepository.findById(1L))
                    .thenThrow(repositoryException);

            RuntimeException exception = assertThrows(
                    RuntimeException.class,
                    () -> categoryService.getCategoryById(1L));

            assertSame(repositoryException, exception);
            verify(categoryRepository).findById(1L);
        }
    }

    @Nested
    @DisplayName("Get Categories By Type Tests")
    class GetCategoriesByTypeTests {

        @Test
        @DisplayName("Returns categories matching transaction type")
        void returnsCategoriesMatchingTransactionType() {
            Category firstCategory = Category.reconstitute(
                    1L, "Groceries", TransactionType.EXPENSE, true);

            Category secondCategory = Category.reconstitute(
                    2L, "Restaurant", TransactionType.EXPENSE, true);

            List<Category> categories = List.of(
                    firstCategory, secondCategory);

            when(categoryRepository.findAllByType(TransactionType.EXPENSE))
                    .thenReturn(categories);

            List<Category> result = categoryService.getCategoriesByType(
                    TransactionType.EXPENSE);

            assertSame(categories, result);
            assertEquals(2, result.size());

            verify(categoryRepository).findAllByType(TransactionType.EXPENSE);
        }

        @Test
        @DisplayName("Includes inactive categories")
        void includesInactiveCategories() {
            Category activeCategory = Category.reconstitute(
                    1L, "Groceries", TransactionType.EXPENSE, true);

            Category inactiveCategory = Category.reconstitute(
                    2L, "Restaurant", TransactionType.EXPENSE, false);

            List<Category> categories = List.of(
                    activeCategory, inactiveCategory);

            when(categoryRepository.findAllByType(TransactionType.EXPENSE))
                    .thenReturn(categories);

            List<Category> result = categoryService.getCategoriesByType(
                    TransactionType.EXPENSE);

            assertEquals(2, result.size());
            assertTrue(result.get(0).isActive());
            assertFalse(result.get(1).isActive());

            verify(categoryRepository).findAllByType(TransactionType.EXPENSE);
        }

        @Test
        @DisplayName("Returns empty list when no categories match")
        void returnsEmptyListWhenNoCategoriesMatch() {
            when(categoryRepository.findAllByType(TransactionType.INCOME))
                    .thenReturn(List.of());

            List<Category> result = categoryService.getCategoriesByType(
                    TransactionType.INCOME);

            assertTrue(result.isEmpty());
            verify(categoryRepository).findAllByType(TransactionType.INCOME);
        }

        @Test
        @DisplayName("Throws NullPointerException when type is null")
        void throwsNullPointerExceptionWhenTypeIsNull() {
            NullPointerException exception = assertThrows(
                    NullPointerException.class,
                    () -> categoryService.getCategoriesByType(null));

            assertEquals("type must not be null", exception.getMessage());
            verifyNoInteractions(categoryRepository);
        }

        @Test
        @DisplayName("Propagates repository exception when retrieval fails")
        void propagatesRepositoryExceptionWhenRetrievalFails() {
            RuntimeException repositoryException = new RuntimeException("Repository failure");

            when(categoryRepository.findAllByType(TransactionType.EXPENSE))
                    .thenThrow(repositoryException);

            RuntimeException exception = assertThrows(
                    RuntimeException.class,
                    () -> categoryService.getCategoriesByType(
                            TransactionType.EXPENSE));

            assertSame(repositoryException, exception);

            verify(categoryRepository).findAllByType(TransactionType.EXPENSE);
        }
    }
}
