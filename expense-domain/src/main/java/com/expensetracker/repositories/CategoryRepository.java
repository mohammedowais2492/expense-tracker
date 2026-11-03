
package com.expensetracker.repositories;

import com.expensetracker.models.Category;
import com.expensetracker.models.TransactionType;
import java.util.List;
import java.util.Optional;

/**
 * Defines persistence operations for {@link Category} entities.
 *
 * <p>
 * This interface abstracts the underlying persistence mechanism from
 * the application and service layers.
 * </p>
 *
 * <p>
 * Categories are deactivated rather than physically deleted so that
 * existing financial transactions can retain their category references.
 * </p>
 */
public interface CategoryRepository {

    /**
     * Persists a new category.
     *
     * @param category the category to persist
     * @return the persisted category, including its generated ID
     */
    Category save(Category category);

    /**
     * Finds a category by its ID.
     *
     * @param id the ID of the category
     * @return an {@link Optional} containing the category if it exists,
     *         or an empty {@link Optional} otherwise
     */
    Optional<Category> findById(long id);

    /**
     * Retrieves all categories belonging to the specified transaction type.
     *
     * <p>
     * Both active and inactive categories are included.
     * </p>
     *
     * @param type the transaction type associated with the categories
     * @return a list of matching categories, or an empty list if none exist
     */
    List<Category> findAllByType(TransactionType type);

    /**
     * Updates an existing category.
     *
     * @param category the category containing the updated values
     * @return the updated category
     * @throws IllegalArgumentException if the category does not exist
     */
    Category update(Category category);
}
