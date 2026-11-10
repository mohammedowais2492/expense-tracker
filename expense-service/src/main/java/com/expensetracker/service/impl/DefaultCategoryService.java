package com.expensetracker.service.impl;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.expensetracker.models.Category;
import com.expensetracker.models.TransactionType;
import com.expensetracker.repositories.CategoryRepository;
import com.expensetracker.services.CategoryService;

/**
 * Default implementation of {@link CategoryService}.
 *
 * <p>
 * Provides application-level category management operations
 * by delegating persistence to {@link CategoryRepository}.
 *
 * <p>
 * Category transaction types are immutable, while category names
 * and activation states may be updated.
 */
public class DefaultCategoryService implements CategoryService {
    private final CategoryRepository categoryRepository;

    /**
     * Creates a category service backed by the specified repository.
     *
     * @param categoryRepository the repository used to manage categories
     * @throws NullPointerException if categoryRepository is null
     */
    public DefaultCategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = Objects.requireNonNull(categoryRepository, "categoryRepository must not be null");
    }

    @Override
    public Category createCategory(Category category) {
        Objects.requireNonNull(category, "category must not be null");
        if (category.getId() != null) {
            throw new IllegalArgumentException("New category must not have an id");
        }
        return this.categoryRepository.save(category);
    }

    @Override
    public Category updateCategory(Category category) {
        Objects.requireNonNull(category, "category must not be null");
        if (category.getId() == null) {
            throw new IllegalArgumentException("categoryId must not be null");
        }
        Category existingCategory = this.categoryRepository
                .findById(category.getId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Category does not exist: " + category.getId()));

        if (existingCategory.getType() != category.getType()) {
            throw new IllegalArgumentException(
                    "Category transaction type cannot be changed");
        }
        return this.categoryRepository.update(category);
    }

    @Override
    public Optional<Category> getCategoryById(long categoryId) {
        if (categoryId <= 0) {
            throw new IllegalArgumentException("categoryId must be positive");
        }
        return this.categoryRepository.findById(categoryId);
    }

    @Override
    public List<Category> getCategoriesByType(TransactionType type) {
        Objects.requireNonNull(type, "type must not be null");
        return this.categoryRepository.findAllByType(type);
    }
}
