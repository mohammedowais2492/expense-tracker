package com.expensetracker.ui.navigation;

/**
 * Defines a contract for dialog controllers that report whether
 * their associated operation completed successfully.
 *
 * <p>
 * Allows the application to determine the outcome of a dialog
 * after it closes, without depending on a specific dialog controller.
 *
 * <p>
 * A successful result indicates that the dialog's intended operation
 * was completed. Cancelling, closing the dialog without completing
 * the operation, or encountering an error results in an unsuccessful outcome.
 */
public interface DialogResultProvider {

    /**
     * Indicates whether the dialog's operation completed successfully.
     *
     * @return {@code true} if the operation completed successfully;
     *         {@code false} otherwise
     */
    boolean isSuccessful();
}