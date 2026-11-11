package com.expensetracker.ui.bootstrap;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

import javax.sql.DataSource;

import com.expensetracker.persistence.DatabaseConnectionManager;
import com.expensetracker.persistence.DatabaseMigrationManager;
import com.expensetracker.persistence.SQLiteDataSourceFactory;
import com.expensetracker.persistence.repositories.SQLiteCategoryRepository;
import com.expensetracker.persistence.repositories.SQLiteExpenseProfileRepository;
import com.expensetracker.persistence.repositories.SQLiteFinancialTransactionRepository;
import com.expensetracker.persistence.repositories.SQLiteUserRepository;
import com.expensetracker.repositories.CategoryRepository;
import com.expensetracker.repositories.ExpenseProfileRepository;
import com.expensetracker.repositories.FinancialTransactionRepository;
import com.expensetracker.repositories.UserRepository;
import com.expensetracker.service.impl.DefaultCategoryService;
import com.expensetracker.service.impl.DefaultExpenseProfileService;
import com.expensetracker.service.impl.DefaultFinancialTransactionService;
import com.expensetracker.service.impl.DefaultUserService;
import com.expensetracker.services.CategoryService;
import com.expensetracker.services.ExpenseProfileService;
import com.expensetracker.services.FinancialTransactionService;
import com.expensetracker.services.UserService;

/**
 * Initializes the Expense Tracker application's infrastructure
 * and service dependencies.
 *
 * <p>
 * Configures database connectivity, applies pending database migrations,
 * constructs repository implementations, and wires application services
 * into an {@link ApplicationContext}.
 */
public final class ApplicationBootstrap {
    private ApplicationBootstrap() {
        throw new UnsupportedOperationException(
                "Utility class cannot be instantiated");
    }

    /**
     * Initializes application dependencies using the specified SQLite database.
     *
     * @param databasePath path to the SQLite database file
     * @return the initialized application context
     * @throws NullPointerException if databasePath is null
     * @throws IOException          if the database cannot be initialized
     */
    public static ApplicationContext initialize(Path databasePath) throws IOException {
        Objects.requireNonNull(databasePath, "databasePath must not be null");
        DataSource dataSource = SQLiteDataSourceFactory.create(databasePath);
        DatabaseMigrationManager databaseMigrationManager = new DatabaseMigrationManager(dataSource);
        databaseMigrationManager.migrate();
        DatabaseConnectionManager databaseConnectionManager = new DatabaseConnectionManager(dataSource);
        UserRepository userRepository = new SQLiteUserRepository(databaseConnectionManager);
        ExpenseProfileRepository expenseProfileRepository = new SQLiteExpenseProfileRepository(
                databaseConnectionManager);
        CategoryRepository categoryRepository = new SQLiteCategoryRepository(databaseConnectionManager);
        FinancialTransactionRepository financialTransactionRepository = new SQLiteFinancialTransactionRepository(
                databaseConnectionManager);
        UserService userService = new DefaultUserService(userRepository);
        ExpenseProfileService expenseProfileService = new DefaultExpenseProfileService(expenseProfileRepository);
        CategoryService categoryService = new DefaultCategoryService(categoryRepository);
        FinancialTransactionService financialTransactionService = new DefaultFinancialTransactionService(
                categoryRepository, financialTransactionRepository);
        return new ApplicationContext(userService, expenseProfileService, categoryService, financialTransactionService);
    }
}
