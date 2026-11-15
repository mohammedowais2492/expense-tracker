package com.expensetracker.ui.alerts;

/**
 * Defines application-level operations for displaying alerts
 * and requesting user confirmation.
 *
 * <p>
 * Implementations determine how alerts are presented
 * without exposing presentation details to callers.
 */
public interface AlertService {

    /**
     * Displays an error alert.
     *
     * @param title   the alert window title
     * @param header  the primary error message
     * @param message the detailed error message
     */
    void showError(String title, String header, String message);

    /**
     * Requests confirmation from the user.
     *
     * @param title   the alert window title
     * @param header  the confirmation question
     * @param message additional information about the action
     * @return {@code true} only when the user explicitly confirms;
     *         {@code false} when the user cancels or dismisses the alert
     */
    boolean confirm(String title, String header, String message);
}
