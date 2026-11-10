package com.expensetracker.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.expensetracker.models.Category;
import com.expensetracker.models.FinancialTransaction;
import com.expensetracker.models.FinancialTransactionQuery;
import com.expensetracker.models.Necessity;
import com.expensetracker.models.Page;
import com.expensetracker.models.PageRequest;
import com.expensetracker.models.TransactionType;
import com.expensetracker.repositories.CategoryRepository;
import com.expensetracker.repositories.FinancialTransactionRepository;

/**
 * Unit tests for {@link DefaultFinancialTransactionService}.
 *
 * <p>
 * Uses mocked repositories to verify transaction validation,
 * category assignment rules, query construction, pagination,
 * repository delegation, and exception propagation.
 *
 * <p>
 * No database is accessed by these tests.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DefaultFinancialTransactionService Tests")
class DefaultFinancialTransactionServiceTest {

    private static final long PROFILE_ID = 1L;
    private static final long CATEGORY_ID = 10L;
    private static final long TRANSACTION_ID = 100L;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private FinancialTransactionRepository financialTransactionRepository;

    @Mock
    private Page<FinancialTransaction> page;

    private DefaultFinancialTransactionService transactionService;

    @BeforeEach
    void setUp() {
        transactionService = new DefaultFinancialTransactionService(
                categoryRepository,
                financialTransactionRepository);
    }

    private FinancialTransaction createExpense() {
        return FinancialTransaction.builder()
                .profileId(PROFILE_ID)
                .categoryId(CATEGORY_ID)
                .type(TransactionType.EXPENSE)
                .amount(2500L)
                .transactionDate(LocalDate.of(2026, 11, 10))
                .necessity(Necessity.NECESSARY)
                .description("Groceries")
                .build();
    }

    private FinancialTransaction createIncome() {
        return FinancialTransaction.builder()
                .profileId(PROFILE_ID)
                .categoryId(CATEGORY_ID)
                .type(TransactionType.INCOME)
                .amount(100000L)
                .transactionDate(LocalDate.of(2026, 11, 10))
                .description("Salary")
                .build();
    }

    private FinancialTransaction persistedExpense() {
        return FinancialTransaction.reconstitute(
                TRANSACTION_ID,
                PROFILE_ID,
                CATEGORY_ID,
                2500L,
                TransactionType.EXPENSE,
                LocalDate.of(2026, 11, 10),
                "Groceries",
                Necessity.NECESSARY);
    }

    private Category category(
            long categoryId,
            TransactionType type,
            boolean active) {

        return Category.reconstitute(
                categoryId,
                "Category",
                type,
                active);
    }

    private PageRequest pageRequest() {
        return PageRequest.builder()
                .pageSize(10)
                .build();
    }

    private void mockFindAll(PageRequest request) {
        when(financialTransactionRepository.findAll(
                any(FinancialTransactionQuery.class),
                same(request)))
                .thenReturn(page);
    }

    private void assertQuery(
            PageRequest request,
            long expectedProfileId,
            TransactionType expectedType,
            Necessity expectedNecessity,
            LocalDate expectedStartDate,
            LocalDate expectedEndDate) {

        ArgumentCaptor<FinancialTransactionQuery> captor = ArgumentCaptor.forClass(FinancialTransactionQuery.class);

        verify(financialTransactionRepository).findAll(
                captor.capture(),
                same(request));

        FinancialTransactionQuery query = captor.getValue();

        assertEquals(expectedProfileId, query.getProfileId());
        assertEquals(expectedType, query.getType());
        assertEquals(expectedNecessity, query.getNecessity());
        assertEquals(expectedStartDate, query.getStartDate());
        assertEquals(expectedEndDate, query.getEndDate());
        assertNull(query.getCategoryId());
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Creates service with valid repositories")
        void createsServiceWithValidRepositories() {
            DefaultFinancialTransactionService service = new DefaultFinancialTransactionService(
                    categoryRepository,
                    financialTransactionRepository);

            assertNotNull(service);
        }

        @Test
        @DisplayName("Throws NullPointerException when category repository is null")
        void throwsNullPointerExceptionWhenCategoryRepositoryIsNull() {
            NullPointerException exception = assertThrows(
                    NullPointerException.class,
                    () -> new DefaultFinancialTransactionService(
                            null,
                            financialTransactionRepository));

            assertEquals(
                    "categoryRepository must not be null",
                    exception.getMessage());
        }

        @Test
        @DisplayName("Throws NullPointerException when transaction repository is null")
        void throwsNullPointerExceptionWhenTransactionRepositoryIsNull() {
            NullPointerException exception = assertThrows(
                    NullPointerException.class,
                    () -> new DefaultFinancialTransactionService(
                            categoryRepository,
                            null));

            assertEquals(
                    "financialTransactionRepository must not be null",
                    exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Create Financial Transaction Tests")
    class CreateFinancialTransactionTests {

        @Test
        @DisplayName("Creates expense with matching active category")
        void createsExpenseWithMatchingActiveCategory() {
            FinancialTransaction transaction = createExpense();

            FinancialTransaction persisted = persistedExpense();

            when(categoryRepository.findById(CATEGORY_ID))
                    .thenReturn(Optional.of(category(
                            CATEGORY_ID,
                            TransactionType.EXPENSE,
                            true)));

            when(financialTransactionRepository.save(transaction))
                    .thenReturn(persisted);

            FinancialTransaction result = transactionService.createFinancialTransaction(transaction);

            assertSame(persisted, result);

            verify(categoryRepository).findById(CATEGORY_ID);
            verify(financialTransactionRepository).save(transaction);
        }

        @Test
        @DisplayName("Creates income with matching active category")
        void createsIncomeWithMatchingActiveCategory() {
            FinancialTransaction transaction = createIncome();

            FinancialTransaction persisted = FinancialTransaction.reconstitute(
                    TRANSACTION_ID,
                    PROFILE_ID,
                    CATEGORY_ID,
                    100000L,
                    TransactionType.INCOME,
                    LocalDate.of(2026, 11, 10),
                    "Salary",
                    null);

            when(categoryRepository.findById(CATEGORY_ID))
                    .thenReturn(Optional.of(category(
                            CATEGORY_ID,
                            TransactionType.INCOME,
                            true)));

            when(financialTransactionRepository.save(transaction))
                    .thenReturn(persisted);

            FinancialTransaction result = transactionService.createFinancialTransaction(transaction);

            assertSame(persisted, result);
            verify(financialTransactionRepository).save(transaction);
        }

        @Test
        @DisplayName("Throws NullPointerException when transaction is null")
        void throwsNullPointerExceptionWhenTransactionIsNull() {
            NullPointerException exception = assertThrows(
                    NullPointerException.class,
                    () -> transactionService.createFinancialTransaction(null));

            assertEquals(
                    "transaction must not be null",
                    exception.getMessage());

            verifyNoInteractions(
                    categoryRepository,
                    financialTransactionRepository);
        }

        @Test
        @DisplayName("Throws IllegalArgumentException when transaction already has ID")
        void throwsIllegalArgumentExceptionWhenTransactionAlreadyHasId() {
            FinancialTransaction transaction = persistedExpense();

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> transactionService.createFinancialTransaction(
                            transaction));

            assertEquals(
                    "New transaction must not have an id",
                    exception.getMessage());

            verifyNoInteractions(
                    categoryRepository,
                    financialTransactionRepository);
        }

        @Test
        @DisplayName("Throws IllegalArgumentException when category does not exist")
        void throwsIllegalArgumentExceptionWhenCategoryDoesNotExist() {
            FinancialTransaction transaction = createExpense();

            when(categoryRepository.findById(CATEGORY_ID))
                    .thenReturn(Optional.empty());

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> transactionService.createFinancialTransaction(
                            transaction));

            assertEquals(
                    "Category does not exist with id: " + CATEGORY_ID,
                    exception.getMessage());

            verify(categoryRepository).findById(CATEGORY_ID);
            verifyNoInteractions(financialTransactionRepository);
        }

        @Test
        @DisplayName("Throws IllegalArgumentException when category is inactive")
        void throwsIllegalArgumentExceptionWhenCategoryIsInactive() {
            FinancialTransaction transaction = createExpense();

            when(categoryRepository.findById(CATEGORY_ID))
                    .thenReturn(Optional.of(category(
                            CATEGORY_ID,
                            TransactionType.EXPENSE,
                            false)));

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> transactionService.createFinancialTransaction(
                            transaction));

            assertEquals(
                    "Cannot create transaction with an inactive category",
                    exception.getMessage());

            verifyNoInteractions(financialTransactionRepository);
        }

        @Test
        @DisplayName("Throws IllegalArgumentException when category type does not match")
        void throwsIllegalArgumentExceptionWhenCategoryTypeDoesNotMatch() {
            FinancialTransaction transaction = createExpense();

            when(categoryRepository.findById(CATEGORY_ID))
                    .thenReturn(Optional.of(category(
                            CATEGORY_ID,
                            TransactionType.INCOME,
                            true)));

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> transactionService.createFinancialTransaction(
                            transaction));

            assertEquals(
                    "Category type does not match transaction type",
                    exception.getMessage());

            verifyNoInteractions(financialTransactionRepository);
        }

        @Test
        @DisplayName("Propagates repository exception when category lookup fails")
        void propagatesRepositoryExceptionWhenCategoryLookupFails() {
            FinancialTransaction transaction = createExpense();

            RuntimeException failure = new RuntimeException("Category lookup failure");

            when(categoryRepository.findById(CATEGORY_ID))
                    .thenThrow(failure);

            RuntimeException exception = assertThrows(
                    RuntimeException.class,
                    () -> transactionService.createFinancialTransaction(
                            transaction));

            assertSame(failure, exception);
            verifyNoInteractions(financialTransactionRepository);
        }

        @Test
        @DisplayName("Propagates repository exception when save fails")
        void propagatesRepositoryExceptionWhenSaveFails() {
            FinancialTransaction transaction = createExpense();

            RuntimeException failure = new RuntimeException("Save failure");

            when(categoryRepository.findById(CATEGORY_ID))
                    .thenReturn(Optional.of(category(
                            CATEGORY_ID,
                            TransactionType.EXPENSE,
                            true)));

            when(financialTransactionRepository.save(transaction))
                    .thenThrow(failure);

            RuntimeException exception = assertThrows(
                    RuntimeException.class,
                    () -> transactionService.createFinancialTransaction(
                            transaction));

            assertSame(failure, exception);
            verify(financialTransactionRepository).save(transaction);
        }
    }

    @Nested
    @DisplayName("Get Financial Transaction By ID Tests")
    class GetFinancialTransactionByIdTests {

        @Test
        @DisplayName("Returns transaction when ID exists")
        void returnsTransactionWhenIdExists() {
            FinancialTransaction transaction = persistedExpense();

            when(financialTransactionRepository.findById(TRANSACTION_ID))
                    .thenReturn(Optional.of(transaction));

            Optional<FinancialTransaction> result = transactionService.getFinancialTransactionById(
                    TRANSACTION_ID);

            assertTrue(result.isPresent());
            assertSame(transaction, result.get());

            verify(financialTransactionRepository)
                    .findById(TRANSACTION_ID);
        }

        @Test
        @DisplayName("Returns empty Optional when transaction does not exist")
        void returnsEmptyOptionalWhenTransactionDoesNotExist() {
            when(financialTransactionRepository.findById(TRANSACTION_ID))
                    .thenReturn(Optional.empty());

            Optional<FinancialTransaction> result = transactionService.getFinancialTransactionById(
                    TRANSACTION_ID);

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Throws IllegalArgumentException when ID is zero")
        void throwsIllegalArgumentExceptionWhenIdIsZero() {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> transactionService.getFinancialTransactionById(0));

            verifyNoInteractions(financialTransactionRepository);
        }

        @Test
        @DisplayName("Throws IllegalArgumentException when ID is negative")
        void throwsIllegalArgumentExceptionWhenIdIsNegative() {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> transactionService.getFinancialTransactionById(-1));

            verifyNoInteractions(financialTransactionRepository);
        }

        @Test
        @DisplayName("Propagates repository exception when lookup fails")
        void propagatesRepositoryExceptionWhenLookupFails() {
            RuntimeException failure = new RuntimeException("Lookup failure");

            when(financialTransactionRepository.findById(TRANSACTION_ID))
                    .thenThrow(failure);

            RuntimeException exception = assertThrows(
                    RuntimeException.class,
                    () -> transactionService.getFinancialTransactionById(
                            TRANSACTION_ID));

            assertSame(failure, exception);
        }
    }

    @Nested
    @DisplayName("Get Financial Transactions Of Type Tests")
    class GetFinancialTransactionsOfTypeTests {

        @Test
        @DisplayName("Constructs query for expense transactions")
        void constructsQueryForExpenseTransactions() {
            PageRequest request = pageRequest();
            mockFindAll(request);

            Page<FinancialTransaction> result = transactionService.getFinancialTransactionsOfType(
                    PROFILE_ID,
                    TransactionType.EXPENSE,
                    request);

            assertSame(page, result);

            assertQuery(
                    request,
                    PROFILE_ID,
                    TransactionType.EXPENSE,
                    null,
                    null,
                    null);
        }

        @Test
        @DisplayName("Constructs query for income transactions")
        void constructsQueryForIncomeTransactions() {
            PageRequest request = pageRequest();
            mockFindAll(request);

            Page<FinancialTransaction> result = transactionService.getFinancialTransactionsOfType(
                    PROFILE_ID,
                    TransactionType.INCOME,
                    request);

            assertSame(page, result);

            assertQuery(
                    request,
                    PROFILE_ID,
                    TransactionType.INCOME,
                    null,
                    null,
                    null);
        }

        @Test
        @DisplayName("Throws IllegalArgumentException when profile ID is invalid")
        void throwsIllegalArgumentExceptionWhenProfileIdIsInvalid() {
            PageRequest request = pageRequest();

            assertThrows(
                    IllegalArgumentException.class,
                    () -> transactionService.getFinancialTransactionsOfType(
                            0,
                            TransactionType.EXPENSE,
                            request));

            assertThrows(
                    IllegalArgumentException.class,
                    () -> transactionService.getFinancialTransactionsOfType(
                            -1,
                            TransactionType.EXPENSE,
                            request));

            verifyNoInteractions(financialTransactionRepository);
        }

        @Test
        @DisplayName("Throws NullPointerException when type is null")
        void throwsNullPointerExceptionWhenTypeIsNull() {
            assertThrows(
                    NullPointerException.class,
                    () -> transactionService.getFinancialTransactionsOfType(
                            PROFILE_ID,
                            null,
                            pageRequest()));

            verifyNoInteractions(financialTransactionRepository);
        }

        @Test
        @DisplayName("Throws NullPointerException when page request is null")
        void throwsNullPointerExceptionWhenPageRequestIsNull() {
            assertThrows(
                    NullPointerException.class,
                    () -> transactionService.getFinancialTransactionsOfType(
                            PROFILE_ID,
                            TransactionType.EXPENSE,
                            null));

            verifyNoInteractions(financialTransactionRepository);
        }
    }

    @Nested
    @DisplayName("Get Necessary Expenses Tests")
    class GetNecessaryExpensesTests {

        @Test
        @DisplayName("Constructs query for necessary expenses")
        void constructsQueryForNecessaryExpenses() {
            PageRequest request = pageRequest();
            mockFindAll(request);

            Page<FinancialTransaction> result = transactionService.getNecessaryExpenses(
                    PROFILE_ID,
                    request);

            assertSame(page, result);

            assertQuery(
                    request,
                    PROFILE_ID,
                    TransactionType.EXPENSE,
                    Necessity.NECESSARY,
                    null,
                    null);
        }

        @Test
        @DisplayName("Throws IllegalArgumentException when profile ID is invalid")
        void throwsIllegalArgumentExceptionWhenProfileIdIsInvalid() {
            PageRequest request = pageRequest();

            assertThrows(
                    IllegalArgumentException.class,
                    () -> transactionService.getNecessaryExpenses(0, request));

            assertThrows(
                    IllegalArgumentException.class,
                    () -> transactionService.getNecessaryExpenses(-1, request));

            verifyNoInteractions(financialTransactionRepository);
        }

        @Test
        @DisplayName("Throws NullPointerException when page request is null")
        void throwsNullPointerExceptionWhenPageRequestIsNull() {
            assertThrows(
                    NullPointerException.class,
                    () -> transactionService.getNecessaryExpenses(
                            PROFILE_ID,
                            null));

            verifyNoInteractions(financialTransactionRepository);
        }
    }

    @Nested
    @DisplayName("Get Unnecessary Expenses Tests")
    class GetUnnecessaryExpensesTests {

        @Test
        @DisplayName("Constructs query for unnecessary expenses")
        void constructsQueryForUnnecessaryExpenses() {
            PageRequest request = pageRequest();
            mockFindAll(request);

            Page<FinancialTransaction> result = transactionService.getUnnecessaryExpenses(
                    PROFILE_ID,
                    request);

            assertSame(page, result);

            assertQuery(
                    request,
                    PROFILE_ID,
                    TransactionType.EXPENSE,
                    Necessity.UNNECESSARY,
                    null,
                    null);
        }

        @Test
        @DisplayName("Throws IllegalArgumentException when profile ID is invalid")
        void throwsIllegalArgumentExceptionWhenProfileIdIsInvalid() {
            PageRequest request = pageRequest();

            assertThrows(
                    IllegalArgumentException.class,
                    () -> transactionService.getUnnecessaryExpenses(0, request));

            assertThrows(
                    IllegalArgumentException.class,
                    () -> transactionService.getUnnecessaryExpenses(-1, request));

            verifyNoInteractions(financialTransactionRepository);
        }

        @Test
        @DisplayName("Throws NullPointerException when page request is null")
        void throwsNullPointerExceptionWhenPageRequestIsNull() {
            assertThrows(
                    NullPointerException.class,
                    () -> transactionService.getUnnecessaryExpenses(
                            PROFILE_ID,
                            null));

            verifyNoInteractions(financialTransactionRepository);
        }
    }

    @Nested
    @DisplayName("Get Financial Transactions For Month Tests")
    class GetFinancialTransactionsForMonthTests {

        @Test
        @DisplayName("Constructs query with inclusive month boundaries")
        void constructsQueryWithInclusiveMonthBoundaries() {
            PageRequest request = pageRequest();
            mockFindAll(request);

            Page<FinancialTransaction> result = transactionService.getFinancialTransactionsForMonth(
                    PROFILE_ID,
                    YearMonth.of(2026, 11),
                    request);

            assertSame(page, result);

            assertQuery(
                    request,
                    PROFILE_ID,
                    TransactionType.EXPENSE,
                    null,
                    LocalDate.of(2026, 11, 1),
                    LocalDate.of(2026, 11, 30));
        }

        @Test
        @DisplayName("Handles February in a leap year")
        void handlesFebruaryInLeapYear() {
            PageRequest request = pageRequest();
            mockFindAll(request);

            transactionService.getFinancialTransactionsForMonth(
                    PROFILE_ID,
                    YearMonth.of(2024, 2),
                    request);

            assertQuery(
                    request,
                    PROFILE_ID,
                    TransactionType.EXPENSE,
                    null,
                    LocalDate.of(2024, 2, 1),
                    LocalDate.of(2024, 2, 29));
        }

        @Test
        @DisplayName("Handles February in a non-leap year")
        void handlesFebruaryInNonLeapYear() {
            PageRequest request = pageRequest();
            mockFindAll(request);

            transactionService.getFinancialTransactionsForMonth(
                    PROFILE_ID,
                    YearMonth.of(2025, 2),
                    request);

            assertQuery(
                    request,
                    PROFILE_ID,
                    TransactionType.EXPENSE,
                    null,
                    LocalDate.of(2025, 2, 1),
                    LocalDate.of(2025, 2, 28));
        }

        @Test
        @DisplayName("Throws IllegalArgumentException when profile ID is invalid")
        void throwsIllegalArgumentExceptionWhenProfileIdIsInvalid() {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> transactionService.getFinancialTransactionsForMonth(
                            0,
                            YearMonth.of(2026, 11),
                            pageRequest()));

            verifyNoInteractions(financialTransactionRepository);
        }

        @Test
        @DisplayName("Throws NullPointerException when month is null")
        void throwsNullPointerExceptionWhenMonthIsNull() {
            assertThrows(
                    NullPointerException.class,
                    () -> transactionService.getFinancialTransactionsForMonth(
                            PROFILE_ID,
                            null,
                            pageRequest()));

            verifyNoInteractions(financialTransactionRepository);
        }

        @Test
        @DisplayName("Throws NullPointerException when page request is null")
        void throwsNullPointerExceptionWhenPageRequestIsNull() {
            assertThrows(
                    NullPointerException.class,
                    () -> transactionService.getFinancialTransactionsForMonth(
                            PROFILE_ID,
                            YearMonth.of(2026, 11),
                            null));

            verifyNoInteractions(financialTransactionRepository);
        }
    }

    @Nested
    @DisplayName("Get Financial Transactions For Year Tests")
    class GetFinancialTransactionsForYearTests {

        @Test
        @DisplayName("Constructs query with inclusive year boundaries")
        void constructsQueryWithInclusiveYearBoundaries() {
            PageRequest request = pageRequest();
            mockFindAll(request);

            Page<FinancialTransaction> result = transactionService.getFinancialTransactionsForYear(
                    PROFILE_ID,
                    2026,
                    request);

            assertSame(page, result);

            assertQuery(
                    request,
                    PROFILE_ID,
                    TransactionType.EXPENSE,
                    null,
                    LocalDate.of(2026, 1, 1),
                    LocalDate.of(2026, 12, 31));
        }

        @Test
        @DisplayName("Constructs query for leap year")
        void constructsQueryForLeapYear() {
            PageRequest request = pageRequest();
            mockFindAll(request);

            transactionService.getFinancialTransactionsForYear(
                    PROFILE_ID,
                    2024,
                    request);

            assertQuery(
                    request,
                    PROFILE_ID,
                    TransactionType.EXPENSE,
                    null,
                    LocalDate.of(2024, 1, 1),
                    LocalDate.of(2024, 12, 31));
        }

        @Test
        @DisplayName("Throws IllegalArgumentException when profile ID is invalid")
        void throwsIllegalArgumentExceptionWhenProfileIdIsInvalid() {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> transactionService.getFinancialTransactionsForYear(
                            0,
                            2026,
                            pageRequest()));

            verifyNoInteractions(financialTransactionRepository);
        }

        @Test
        @DisplayName("Throws NullPointerException when page request is null")
        void throwsNullPointerExceptionWhenPageRequestIsNull() {
            assertThrows(
                    NullPointerException.class,
                    () -> transactionService.getFinancialTransactionsForYear(
                            PROFILE_ID,
                            2026,
                            null));

            verifyNoInteractions(financialTransactionRepository);
        }
    }

    @Nested
    @DisplayName("Get Filtered Financial Transactions Tests")
    class GetFilteredFinancialTransactionsTests {

        @Test
        @DisplayName("Delegates query and pagination to repository")
        void delegatesQueryAndPaginationToRepository() {
            FinancialTransactionQuery query = FinancialTransactionQuery.builder()
                    .profileId(PROFILE_ID)
                    .type(TransactionType.EXPENSE)
                    .categoryId(CATEGORY_ID)
                    .necessity(Necessity.NECESSARY)
                    .startDate(LocalDate.of(2026, 1, 1))
                    .endDate(LocalDate.of(2026, 12, 31))
                    .build();

            PageRequest request = pageRequest();

            when(financialTransactionRepository.findAll(query, request))
                    .thenReturn(page);

            Page<FinancialTransaction> result = transactionService.getFilteredFinancialTransactions(
                    query,
                    request);

            assertSame(page, result);
            verify(financialTransactionRepository).findAll(query, request);
        }

        @Test
        @DisplayName("Throws NullPointerException when query is null")
        void throwsNullPointerExceptionWhenQueryIsNull() {
            NullPointerException exception = assertThrows(
                    NullPointerException.class,
                    () -> transactionService.getFilteredFinancialTransactions(
                            null,
                            pageRequest()));

            assertEquals(
                    "financialTransactionQuery must not be null",
                    exception.getMessage());

            verifyNoInteractions(financialTransactionRepository);
        }

        @Test
        @DisplayName("Throws NullPointerException when page request is null")
        void throwsNullPointerExceptionWhenPageRequestIsNull() {
            FinancialTransactionQuery query = FinancialTransactionQuery.builder()
                    .profileId(PROFILE_ID)
                    .type(TransactionType.EXPENSE)
                    .build();

            assertThrows(
                    NullPointerException.class,
                    () -> transactionService.getFilteredFinancialTransactions(
                            query,
                            null));

            verifyNoInteractions(financialTransactionRepository);
        }

        @Test
        @DisplayName("Propagates repository exception when query fails")
        void propagatesRepositoryExceptionWhenQueryFails() {
            FinancialTransactionQuery query = FinancialTransactionQuery.builder()
                    .profileId(PROFILE_ID)
                    .type(TransactionType.EXPENSE)
                    .build();

            PageRequest request = pageRequest();

            RuntimeException failure = new RuntimeException("Query failure");

            when(financialTransactionRepository.findAll(query, request))
                    .thenThrow(failure);

            RuntimeException exception = assertThrows(
                    RuntimeException.class,
                    () -> transactionService.getFilteredFinancialTransactions(
                            query,
                            request));

            assertSame(failure, exception);
        }
    }

    @Nested
    @DisplayName("Update Transaction Tests")
    class UpdateTransactionTests {

        @Test
        @DisplayName("Updates transaction with unchanged active category")
        void updatesTransactionWithUnchangedActiveCategory() {
            FinancialTransaction transaction = persistedExpense();

            when(financialTransactionRepository.findById(TRANSACTION_ID))
                    .thenReturn(Optional.of(transaction));

            when(categoryRepository.findById(CATEGORY_ID))
                    .thenReturn(Optional.of(category(
                            CATEGORY_ID,
                            TransactionType.EXPENSE,
                            true)));

            when(financialTransactionRepository.update(transaction))
                    .thenReturn(transaction);

            FinancialTransaction result = transactionService.updateTransaction(transaction);

            assertSame(transaction, result);

            verify(financialTransactionRepository)
                    .findById(TRANSACTION_ID);
            verify(categoryRepository).findById(CATEGORY_ID);
            verify(financialTransactionRepository).update(transaction);
        }

        @Test
        @DisplayName("Allows update with unchanged inactive category")
        void allowsUpdateWithUnchangedInactiveCategory() {
            FinancialTransaction existing = persistedExpense();

            FinancialTransaction updated = FinancialTransaction.reconstitute(
                    TRANSACTION_ID,
                    PROFILE_ID,
                    CATEGORY_ID,
                    3000L,
                    TransactionType.EXPENSE,
                    LocalDate.of(2026, 11, 10),
                    "Corrected amount",
                    Necessity.NECESSARY);

            when(financialTransactionRepository.findById(TRANSACTION_ID))
                    .thenReturn(Optional.of(existing));

            when(categoryRepository.findById(CATEGORY_ID))
                    .thenReturn(Optional.of(category(
                            CATEGORY_ID,
                            TransactionType.EXPENSE,
                            false)));

            when(financialTransactionRepository.update(updated))
                    .thenReturn(updated);

            FinancialTransaction result = transactionService.updateTransaction(updated);

            assertSame(updated, result);
            verify(financialTransactionRepository).update(updated);
        }

        @Test
        @DisplayName("Allows assigning a different active category")
        void allowsAssigningDifferentActiveCategory() {
            FinancialTransaction existing = persistedExpense();

            FinancialTransaction updated = FinancialTransaction.reconstitute(
                    TRANSACTION_ID,
                    PROFILE_ID,
                    20L,
                    2500L,
                    TransactionType.EXPENSE,
                    LocalDate.of(2026, 11, 10),
                    "Groceries",
                    Necessity.NECESSARY);

            when(financialTransactionRepository.findById(TRANSACTION_ID))
                    .thenReturn(Optional.of(existing));

            when(categoryRepository.findById(20L))
                    .thenReturn(Optional.of(category(
                            20L,
                            TransactionType.EXPENSE,
                            true)));

            when(financialTransactionRepository.update(updated))
                    .thenReturn(updated);

            FinancialTransaction result = transactionService.updateTransaction(updated);

            assertSame(updated, result);
            verify(categoryRepository).findById(20L);
            verify(financialTransactionRepository).update(updated);
        }

        @Test
        @DisplayName("Allows moving transaction to another profile")
        void allowsMovingTransactionToAnotherProfile() {
            FinancialTransaction existing = persistedExpense();

            FinancialTransaction moved = FinancialTransaction.reconstitute(
                    TRANSACTION_ID,
                    2L,
                    CATEGORY_ID,
                    2500L,
                    TransactionType.EXPENSE,
                    LocalDate.of(2026, 11, 10),
                    "Groceries",
                    Necessity.NECESSARY);

            when(financialTransactionRepository.findById(TRANSACTION_ID))
                    .thenReturn(Optional.of(existing));

            when(categoryRepository.findById(CATEGORY_ID))
                    .thenReturn(Optional.of(category(
                            CATEGORY_ID,
                            TransactionType.EXPENSE,
                            true)));

            when(financialTransactionRepository.update(moved))
                    .thenReturn(moved);

            FinancialTransaction result = transactionService.updateTransaction(moved);

            assertEquals(2L, result.getProfileId());
            assertEquals(TRANSACTION_ID, result.getId());
            verify(financialTransactionRepository).update(moved);
        }

        @Test
        @DisplayName("Allows moving transaction while retaining inactive category")
        void allowsMovingTransactionWhileRetainingInactiveCategory() {
            FinancialTransaction existing = persistedExpense();

            FinancialTransaction moved = FinancialTransaction.reconstitute(
                    TRANSACTION_ID,
                    2L,
                    CATEGORY_ID,
                    2500L,
                    TransactionType.EXPENSE,
                    LocalDate.of(2026, 11, 10),
                    "Groceries",
                    Necessity.NECESSARY);

            when(financialTransactionRepository.findById(TRANSACTION_ID))
                    .thenReturn(Optional.of(existing));

            when(categoryRepository.findById(CATEGORY_ID))
                    .thenReturn(Optional.of(category(
                            CATEGORY_ID,
                            TransactionType.EXPENSE,
                            false)));

            when(financialTransactionRepository.update(moved))
                    .thenReturn(moved);

            FinancialTransaction result = transactionService.updateTransaction(moved);

            assertSame(moved, result);
            verify(financialTransactionRepository).update(moved);
        }

        @Test
        @DisplayName("Allows updating income transaction")
        void allowsUpdatingIncomeTransaction() {
            FinancialTransaction income = FinancialTransaction.reconstitute(
                    TRANSACTION_ID,
                    PROFILE_ID,
                    CATEGORY_ID,
                    100000L,
                    TransactionType.INCOME,
                    LocalDate.of(2026, 11, 10),
                    "Salary",
                    null);

            when(financialTransactionRepository.findById(TRANSACTION_ID))
                    .thenReturn(Optional.of(income));

            when(categoryRepository.findById(CATEGORY_ID))
                    .thenReturn(Optional.of(category(
                            CATEGORY_ID,
                            TransactionType.INCOME,
                            true)));

            when(financialTransactionRepository.update(income))
                    .thenReturn(income);

            FinancialTransaction result = transactionService.updateTransaction(income);

            assertSame(income, result);
            verify(financialTransactionRepository).update(income);
        }

        @Test
        @DisplayName("Throws NullPointerException when transaction is null")
        void throwsNullPointerExceptionWhenTransactionIsNull() {
            assertThrows(
                    NullPointerException.class,
                    () -> transactionService.updateTransaction(null));

            verifyNoInteractions(
                    categoryRepository,
                    financialTransactionRepository);
        }

        @Test
        @DisplayName("Throws IllegalArgumentException when transaction has no ID")
        void throwsIllegalArgumentExceptionWhenTransactionHasNoId() {
            FinancialTransaction transaction = createExpense();

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> transactionService.updateTransaction(transaction));

            assertEquals(
                    "Transaction must have an id",
                    exception.getMessage());

            verifyNoInteractions(
                    categoryRepository,
                    financialTransactionRepository);
        }

        @Test
        @DisplayName("Throws IllegalArgumentException when transaction does not exist")
        void throwsIllegalArgumentExceptionWhenTransactionDoesNotExist() {
            FinancialTransaction transaction = persistedExpense();

            when(financialTransactionRepository.findById(TRANSACTION_ID))
                    .thenReturn(Optional.empty());

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> transactionService.updateTransaction(transaction));

            assertEquals(
                    "Transaction does not exist: " + TRANSACTION_ID,
                    exception.getMessage());

            verifyNoInteractions(categoryRepository);

            verify(financialTransactionRepository)
                    .findById(TRANSACTION_ID);
            verifyNoMoreInteractions(financialTransactionRepository);
        }

        @Test
        @DisplayName("Throws IllegalArgumentException when category does not exist")
        void throwsIllegalArgumentExceptionWhenCategoryDoesNotExist() {
            FinancialTransaction transaction = persistedExpense();

            when(financialTransactionRepository.findById(TRANSACTION_ID))
                    .thenReturn(Optional.of(transaction));

            when(categoryRepository.findById(CATEGORY_ID))
                    .thenReturn(Optional.empty());

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> transactionService.updateTransaction(transaction));

            assertEquals(
                    "Category does not exist with id: " + CATEGORY_ID,
                    exception.getMessage());

            verify(financialTransactionRepository)
                    .findById(TRANSACTION_ID);
            verifyNoMoreInteractions(financialTransactionRepository);
        }

        @Test
        @DisplayName("Throws IllegalArgumentException when category type does not match")
        void throwsIllegalArgumentExceptionWhenCategoryTypeDoesNotMatch() {
            FinancialTransaction transaction = persistedExpense();

            when(financialTransactionRepository.findById(TRANSACTION_ID))
                    .thenReturn(Optional.of(transaction));

            when(categoryRepository.findById(CATEGORY_ID))
                    .thenReturn(Optional.of(category(
                            CATEGORY_ID,
                            TransactionType.INCOME,
                            true)));

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> transactionService.updateTransaction(transaction));

            assertEquals(
                    "Category type does not match transaction type",
                    exception.getMessage());

            verify(financialTransactionRepository)
                    .findById(TRANSACTION_ID);
            verifyNoMoreInteractions(financialTransactionRepository);
        }

        @Test
        @DisplayName("Throws IllegalArgumentException when assigning different inactive category")
        void throwsIllegalArgumentExceptionWhenAssigningDifferentInactiveCategory() {
            FinancialTransaction existing = persistedExpense();

            FinancialTransaction updated = FinancialTransaction.reconstitute(
                    TRANSACTION_ID,
                    PROFILE_ID,
                    20L,
                    2500L,
                    TransactionType.EXPENSE,
                    LocalDate.of(2026, 11, 10),
                    "Groceries",
                    Necessity.NECESSARY);

            when(financialTransactionRepository.findById(TRANSACTION_ID))
                    .thenReturn(Optional.of(existing));

            when(categoryRepository.findById(20L))
                    .thenReturn(Optional.of(category(
                            20L,
                            TransactionType.EXPENSE,
                            false)));

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> transactionService.updateTransaction(updated));

            assertEquals(
                    "Cannot assign an inactive category",
                    exception.getMessage());

            verify(categoryRepository).findById(20L);

            verify(financialTransactionRepository)
                    .findById(TRANSACTION_ID);
            verifyNoMoreInteractions(financialTransactionRepository);
        }

        @Test
        @DisplayName("Propagates repository exception when transaction lookup fails")
        void propagatesRepositoryExceptionWhenTransactionLookupFails() {
            FinancialTransaction transaction = persistedExpense();

            RuntimeException failure = new RuntimeException("Transaction lookup failure");

            when(financialTransactionRepository.findById(TRANSACTION_ID))
                    .thenThrow(failure);

            RuntimeException exception = assertThrows(
                    RuntimeException.class,
                    () -> transactionService.updateTransaction(transaction));

            assertSame(failure, exception);
            verifyNoInteractions(categoryRepository);
        }

        @Test
        @DisplayName("Propagates repository exception when category lookup fails")
        void propagatesRepositoryExceptionWhenCategoryLookupFails() {
            FinancialTransaction transaction = persistedExpense();

            RuntimeException failure = new RuntimeException("Category lookup failure");

            when(financialTransactionRepository.findById(TRANSACTION_ID))
                    .thenReturn(Optional.of(transaction));

            when(categoryRepository.findById(CATEGORY_ID))
                    .thenThrow(failure);

            RuntimeException exception = assertThrows(
                    RuntimeException.class,
                    () -> transactionService.updateTransaction(transaction));

            assertSame(failure, exception);

            verify(financialTransactionRepository)
                    .findById(TRANSACTION_ID);
            verifyNoMoreInteractions(financialTransactionRepository);
        }

        @Test
        @DisplayName("Propagates repository exception when update fails")
        void propagatesRepositoryExceptionWhenUpdateFails() {
            FinancialTransaction transaction = persistedExpense();

            RuntimeException failure = new RuntimeException("Update failure");

            when(financialTransactionRepository.findById(TRANSACTION_ID))
                    .thenReturn(Optional.of(transaction));

            when(categoryRepository.findById(CATEGORY_ID))
                    .thenReturn(Optional.of(category(
                            CATEGORY_ID,
                            TransactionType.EXPENSE,
                            true)));

            when(financialTransactionRepository.update(transaction))
                    .thenThrow(failure);

            RuntimeException exception = assertThrows(
                    RuntimeException.class,
                    () -> transactionService.updateTransaction(transaction));

            assertSame(failure, exception);

            verify(financialTransactionRepository).update(transaction);
        }
    }

    @Nested
    @DisplayName("Delete Transaction Tests")
    class DeleteTransactionTests {

        @Test
        @DisplayName("Returns true when transaction is deleted")
        void returnsTrueWhenTransactionIsDeleted() {
            when(financialTransactionRepository.deleteById(TRANSACTION_ID))
                    .thenReturn(true);

            boolean result = transactionService.deleteTransaction(
                    TRANSACTION_ID);

            assertTrue(result);

            verify(financialTransactionRepository)
                    .deleteById(TRANSACTION_ID);
        }

        @Test
        @DisplayName("Returns false when transaction does not exist")
        void returnsFalseWhenTransactionDoesNotExist() {
            when(financialTransactionRepository.deleteById(TRANSACTION_ID))
                    .thenReturn(false);

            boolean result = transactionService.deleteTransaction(
                    TRANSACTION_ID);

            assertFalse(result);

            verify(financialTransactionRepository)
                    .deleteById(TRANSACTION_ID);
        }

        @Test
        @DisplayName("Throws IllegalArgumentException when ID is zero")
        void throwsIllegalArgumentExceptionWhenIdIsZero() {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> transactionService.deleteTransaction(0));

            verifyNoInteractions(financialTransactionRepository);
        }

        @Test
        @DisplayName("Throws IllegalArgumentException when ID is negative")
        void throwsIllegalArgumentExceptionWhenIdIsNegative() {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> transactionService.deleteTransaction(-1));

            verifyNoInteractions(financialTransactionRepository);
        }

        @Test
        @DisplayName("Propagates repository exception when deletion fails")
        void propagatesRepositoryExceptionWhenDeletionFails() {
            RuntimeException failure = new RuntimeException("Delete failure");

            when(financialTransactionRepository.deleteById(TRANSACTION_ID))
                    .thenThrow(failure);

            RuntimeException exception = assertThrows(
                    RuntimeException.class,
                    () -> transactionService.deleteTransaction(
                            TRANSACTION_ID));

            assertSame(failure, exception);

            verify(financialTransactionRepository)
                    .deleteById(TRANSACTION_ID);
        }
    }
}
