package com.expensetracker.ui.navigation;

import lombok.Getter;

/**
 * Identifies the screens available for navigation in the application.
 *
 * <p>
 * Each screen is associated with an FXML resource defining
 * its visual layout.
 */
public enum Screen {

    /** Screen for selecting or creating an application user. */
    LANDING("/com/expensetracker/ui/screens/landing.fxml"),

    /** Main application dashboard. */
    DASHBOARD("/com/expensetracker/ui/layout/app-shell.fxml");

    /**
     * Classpath location of the screen's FXML resource.
     */
    @Getter
    private final String resourcePath;

    /**
     * Creates a screen definition.
     *
     * @param resourcePath the absolute classpath path to its FXML resource
     */
    Screen(String resourcePath) {
        this.resourcePath = resourcePath;
    }
}
