package com.expensetracker.ui.navigation;

/**
 * Identifies the dialogs available in the application.
 */
public enum Dialog {

    ADD_EXPENSE("/com/expensetracker/ui/dialogs/add-expense.fxml");

    private final String fxmlPath;

    Dialog(String fxmlPath) {
        this.fxmlPath = fxmlPath;
    }

    /**
     * Returns the FXML resource path for this dialog.
     *
     * @return the dialog FXML resource path
     */
    public String getFxmlPath() {
        return fxmlPath;
    }
}