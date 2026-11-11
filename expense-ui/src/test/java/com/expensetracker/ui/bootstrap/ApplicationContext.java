package com.expensetracker.ui.bootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.expensetracker.services.CategoryService;
import com.expensetracker.services.ExpenseProfileService;
import com.expensetracker.services.FinancialTransactionService;
import com.expensetracker.services.UserService;

/**
 * Unit tests for {@link ApplicationContext}.
 *
 * <p>
 * Verifies constructor validation and access to initialized
 * service dependencies.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ApplicationContext Tests")
class ApplicationContextTest {

    @Mock
    private UserService userService;

    @Mock
    private ExpenseProfileService expenseProfileService;

    @Mock
    private CategoryService categoryService;

    @Mock
    private FinancialTransactionService financialTransactionService;

    @Test
    @DisplayName("Creates context with valid services")
    void createsContextWithValidServices() {
        ApplicationContext context = new ApplicationContext(
                userService,
                expenseProfileService,
                categoryService,
                financialTransactionService);

        assertSame(userService, context.getUserService());
        assertSame(expenseProfileService, context.getExpenseProfileService());
        assertSame(categoryService, context.getCategoryService());
        assertSame(
                financialTransactionService,
                context.getFinancialTransactionService());
    }

    @Test
    @DisplayName("Throws NullPointerException when user service is null")
    void throwsNullPointerExceptionWhenUserServiceIsNull() {
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new ApplicationContext(
                        null,
                        expenseProfileService,
                        categoryService,
                        financialTransactionService));

        assertEquals(
                "userService is marked non-null but is null",
                exception.getMessage());
    }

    @Test
    @DisplayName("Throws NullPointerException when expense profile service is null")
    void throwsNullPointerExceptionWhenExpenseProfileServiceIsNull() {
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new ApplicationContext(
                        userService,
                        null,
                        categoryService,
                        financialTransactionService));

        assertEquals(
                "expenseProfileService is marked non-null but is null",
                exception.getMessage());
    }

    @Test
    @DisplayName("Throws NullPointerException when category service is null")
    void throwsNullPointerExceptionWhenCategoryServiceIsNull() {
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new ApplicationContext(
                        userService,
                        expenseProfileService,
                        null,
                        financialTransactionService));

        assertEquals(
                "categoryService is marked non-null but is null",
                exception.getMessage());
    }

    @Test
    @DisplayName("Throws NullPointerException when financial transaction service is null")
    void throwsNullPointerExceptionWhenFinancialTransactionServiceIsNull() {
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new ApplicationContext(
                        userService,
                        expenseProfileService,
                        categoryService,
                        null));

        assertEquals(
                "financialTransactionService is marked non-null but is null",
                exception.getMessage());
    }
}
