package com.expensetracker.ui.converters;

import com.expensetracker.models.Category;

import javafx.util.StringConverter;

/**
 * Converts {@link Category} objects to user-friendly display strings
 * for JavaFX controls.
 */
public final class CategoryConverter extends StringConverter<Category> {

    @Override
    public String toString(Category category) {
        return category == null ? "" : category.getName();
    }

    @Override
    public Category fromString(String value) {
        throw new UnsupportedOperationException(
                "Category lookup by name is not supported");
    }
}