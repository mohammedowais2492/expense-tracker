package com.expensetracker.models;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

/**
 * Represents a category used to classify financial transactions.
 *
 * <p>
 * A category belongs to either an income or expense transaction type
 * and can be activated or deactivated. Newly created categories are active
 * by default.
 *
 * <p>
 * Inactive categories cannot be used for new transactions but remain
 * available for historical transactions.
 *
 * <p>
 * Category name uniqueness is enforced outside this model.
 */
@Value
public class Category {
    Long id;
    String name;
    TransactionType type;
    boolean isActive;

    /**
     * Creates a new active category.
     *
     * @param name the category name; must not be null or blank
     * @param type the transaction type associated with the category; must not be
     *             null
     * @throws NullPointerException     if name or type is null
     * @throws IllegalArgumentException if name is blank
     */
    @Builder
    public Category(@NonNull String name, @NonNull TransactionType type) {
        this(null, name, type, true);
    }

    private Category(Long id, @NonNull String name, @NonNull TransactionType type, boolean isActive) {
        if (id != null && id <= 0) {
            throw new IllegalArgumentException("id cannot be non-positive");
        }
        this.id = id;
        name = name.strip();
        if (name.isBlank()) {
            throw new IllegalArgumentException("name cannot be blank");
        }
        this.name = name;
        this.type = type;
        this.isActive = isActive;
    }

    /**
     * Reconstitutes an existing category from persisted data.
     *
     * @param id       the persisted category identifier; must be positive
     * @param name     the category name; must not be null or blank
     * @param type     the transaction type associated with the category; must not
     *                 be null
     * @param isActive whether the category is active
     * @return the reconstituted category
     * @throws NullPointerException     if name or type is null
     * @throws IllegalArgumentException if id is non-positive or name is blank
     */
    public static Category reconstitute(long id, @NonNull String name, @NonNull TransactionType type,
            boolean isActive) {
        return new Category(id, name, type, isActive);
    }

    /**
     * Returns a new category with the specified name.
     *
     * <p>
     * The original category remains unchanged.
     *
     * @param newName the new category name; must not be null or blank
     * @return a new category with the updated name
     * @throws NullPointerException     if newName is null
     * @throws IllegalArgumentException if newName is blank
     */
    public Category rename(@NonNull String newName) {
        return new Category(this.id, newName, this.type, this.isActive);
    }

    /**
     * Returns a new active version of this category.
     *
     * <p>
     * The original category remains unchanged.
     *
     * @return a new active category
     */
    public Category activate() {
        return new Category(this.id, this.name, this.type, true);
    }

    /**
     * Returns a new inactive version of this category.
     *
     * <p>
     * The original category remains unchanged.
     *
     * @return a new inactive category
     */
    public Category deactivate() {
        return new Category(this.id, this.name, this.type, false);
    }
}
