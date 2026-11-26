package com.expensetracker.ui.categories;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.expensetracker.models.Category;
import com.expensetracker.models.TransactionType;
import com.expensetracker.services.CategoryService;

/**
 * Provides cached category lookups for the desktop application.
 *
 * <p>
 * Maintains an immutable, in-memory snapshot of categories indexed
 * by their identifiers. Both active and inactive categories are retained
 * so that historical transactions can resolve their category information.
 *
 * <p>
 * The cache is populated and updated explicitly through {@link #refresh()}.
 * Category changes made through the application should be followed by
 * a refresh to keep subsequent lookups consistent with persisted data.
 */
public final class CategoryLookupService {
    private final CategoryService categoryService;
    private Map<Long, Category> categoriesById = Map.of();

    /**
     * Creates a category lookup service.
     *
     * @param categoryService service used to retrieve persisted categories
     * @throws NullPointerException if categoryService is null
     */
    public CategoryLookupService(CategoryService categoryService) {
        this.categoryService = Objects.requireNonNull(categoryService, "categoryService must not be null");
    }

    /**
     * Reloads all categories from persistence and replaces the cached snapshot.
     *
     * <p>
     * Both income and expense categories are included, regardless of their
     * active status. The existing snapshot remains unchanged if loading
     * or constructing the replacement snapshot fails.
     */
    public void refresh() {
        categoriesById = Map.copyOf(
                Stream.of(TransactionType.values())
                        .flatMap(type -> categoryService.getCategoriesByType(type).stream())
                        .collect(Collectors.toMap(
                                Category::getId,
                                Function.identity())));
    }

    /**
     * Retrieves a category from the current cached snapshot by its identifier.
     *
     * @param categoryId the category identifier; must be positive
     * @return an {@link Optional} containing the matching category,
     *         or an empty Optional if no matching category exists
     * @throws IllegalArgumentException if categoryId is not positive
     */
    public Optional<Category> lookupCategoryById(long categoryId) {
        if (categoryId <= 0) {
            throw new IllegalArgumentException("categoryId must be positive");
        }
        return Optional.ofNullable(categoriesById.get(categoryId));
    }
}
