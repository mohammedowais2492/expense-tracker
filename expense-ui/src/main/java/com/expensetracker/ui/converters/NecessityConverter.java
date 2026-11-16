package com.expensetracker.ui.converters;

import com.expensetracker.models.Necessity;

import javafx.util.StringConverter;

/**
 * Converts {@link Necessity} values to and from
 * user-friendly display strings for JavaFX controls.
 */
public final class NecessityConverter extends StringConverter<Necessity> {

    @Override
    public String toString(Necessity necessity) {
        if (necessity == null) {
            return "";
        }

        return switch (necessity) {
            case NECESSARY -> "Necessary";
            case UNNECESSARY -> "Unnecessary";
        };
    }

    @Override
    public Necessity fromString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return switch (value) {
            case "Necessary" -> Necessity.NECESSARY;
            case "Unnecessary" -> Necessity.UNNECESSARY;
            default -> throw new IllegalArgumentException(
                    "Unknown necessity: " + value);
        };
    }
}