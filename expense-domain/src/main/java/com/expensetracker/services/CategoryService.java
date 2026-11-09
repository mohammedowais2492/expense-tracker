package com.expensetracker.services;

import java.util.List;
import java.util.Optional;

import com.expensetracker.models.Category;
import com.expensetracker.models.TransactionType;

/**
 * Defines application-level operations for managing financial categories.
 *
 * <p>
 * Provides functionality for creating, retrieving, and updating categories
 * used to classify financial transactions.
 *
 * <p>
 * Categories are associated with a fixed transaction type, either
 * {@link TransactionType#INCOME} or {@link TransactionType#EXPENSE}.
 * Once created, a category's transaction type cannot be changed.
 *
 * <p>
 * Categories may be renamed, activated, or deactivated. Deactivated
 * categories remain available for retrieval to preserve historical
 * transaction information but cannot be assigned to new transactions.
 *
 * <p>
 * Categories cannot be deleted through this service. Deactivation
 * is used instead to prevent further use.
 */
public interface CategoryService {

    /**
     * Creates a new category.
     *
     * <p>
     * Category names must be unique within the same transaction type,
     * regardless of letter case.
     *
     * @param category the category to create; must not be null and must
     *                 not have an ID
     * @return the created category, including its generated ID
     * @throws NullPointerException     if category is null
     * @throws IllegalArgumentException if category already has an ID
     */
    Category createCategory(Category category);

    /**
     * Updates an existing category.
     *
     * <p>
     * Supports renaming, activation, and deactivation. The category's
     * transaction type cannot be changed.
     *
     * @param category the category containing the updated information;
     *                 must not be null and must have an ID
     * @return the updated category
     * @throws NullPointerException     if category is null
     * @throws IllegalArgumentException if category has no ID, does not exist,
     *                                  or attempts to change its transaction type
     */
    Category updateCategory(Category category);

    /**
     * Retrieves a category by its ID.
     *
     * <p>
     * Inactive categories are included in retrieval results.
     *
     * @param categoryId the ID of the category; must be positive
     * @return an Optional containing the category if found, or an empty
     *         Optional otherwise
     * @throws IllegalArgumentException if categoryId is not positive
     */
    Optional<Category> getCategoryById(long categoryId);

    /**
     * Retrieves all categories belonging to the specified transaction type.
     *
     * <p>
     * Both active and inactive categories are returned. Categories
     * are ordered by their IDs in ascending order.
     *
     * @param type the transaction type to filter by; must not be null
     * @return a list of matching categories, or an empty list if none exist
     * @throws NullPointerException if type is null
     */
    List<Category> getCategoriesByType(TransactionType type);
}
