package com.expensetracker.ui.screens.transactions;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import com.expensetracker.models.Category;
import com.expensetracker.models.FinancialTransaction;
import com.expensetracker.models.Necessity;
import com.expensetracker.models.Page;
import com.expensetracker.models.PageRequest;
import com.expensetracker.models.PageRequest.PageRequestBuilder;
import com.expensetracker.ui.categories.CategoryLookupService;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

/**
 * Controls the Transactions screen and displays paginated expense history.
 *
 * <p>
 * Retrieves expense transactions through {@link TransactionHistoryService}
 * and resolves category information using {@link CategoryLookupService}.
 *
 * <p>
 * Manages transaction table presentation and pagination controls.
 */
public final class TransactionsController {
        private final TransactionHistoryService transactionHistoryService;
        private final CategoryLookupService categoryLookupService;
        private static final int PAGE_SIZE = 20;
        private int currentPageNumber = 0;
        private final List<String> pageTokens = new ArrayList<>();
        private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM d, uuuu", Locale.US);

        @FXML
        private TableView<FinancialTransaction> transactionsTable;

        @FXML
        private TableColumn<FinancialTransaction, String> dateColumn;

        @FXML
        private TableColumn<FinancialTransaction, String> categoryColumn;

        @FXML
        private TableColumn<FinancialTransaction, String> descriptionColumn;

        @FXML
        private TableColumn<FinancialTransaction, String> necessityColumn;

        @FXML
        private TableColumn<FinancialTransaction, String> amountColumn;

        @FXML
        private Button previousButton;

        @FXML
        private Button nextButton;

        @FXML
        private Label pageLabel;

        /**
         * Creates the Transactions controller with its required dependencies.
         *
         * @param transactionHistoryService service used to retrieve paginated expenses
         * @param categoryLookupService     service used to resolve transaction
         *                                  categories
         * @throws NullPointerException if either dependency is null
         */
        public TransactionsController(TransactionHistoryService transactionHistoryService,
                        CategoryLookupService categoryLookupService) {
                this.transactionHistoryService = Objects.requireNonNull(transactionHistoryService,
                                "transactionHistoryService must not be null");
                this.categoryLookupService = Objects.requireNonNull(categoryLookupService,
                                "categoryLookupService must not be null");
                pageTokens.add(null);
        }

        @FXML
        private void initialize() {
                transactionsTable.setColumnResizePolicy(
                                TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
                dateColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(
                                cellData.getValue()
                                                .getTransactionDate()
                                                .format(DATE_FORMATTER)));
                categoryColumn.setCellValueFactory(cellData -> {
                        long categoryId = cellData.getValue().getCategoryId();

                        String categoryName = categoryLookupService
                                        .lookupCategoryById(categoryId)
                                        .map(Category::getName)
                                        .orElse("Unknown Category");

                        return new ReadOnlyStringWrapper(categoryName);
                });
                descriptionColumn.setCellValueFactory(cellData -> {
                        String description = cellData.getValue().getDescription();

                        return new ReadOnlyStringWrapper(
                                        description == null || description.isBlank()
                                                        ? "—"
                                                        : description);
                });
                necessityColumn.setCellValueFactory(cellData -> {
                        Necessity necessity = cellData.getValue().getNecessity();

                        return new ReadOnlyStringWrapper(
                                        necessity == Necessity.NECESSARY
                                                        ? "Necessary"
                                                        : "Unnecessary");
                });
                amountColumn.setCellValueFactory(cellData -> {
                        long amountInCents = cellData.getValue().getAmount();

                        BigDecimal amount = BigDecimal.valueOf(amountInCents, 2);

                        NumberFormat formatter = NumberFormat.getCurrencyInstance(Locale.US);

                        return new ReadOnlyStringWrapper(formatter.format(amount));
                });
                loadPage();
        }

        // TODO: Handle pagination failures gracefully. Currently, the Previous and
        // Next handlers update currentPageNumber before loading the requested page.
        // If loading fails, the page index may become inconsistent with the displayed
        // transactions. Update pagination state only after a successful load, or
        // restore the previous state on failure.
        @FXML
        private void handlePrevious() {
                if (currentPageNumber > 0) {
                        currentPageNumber--;
                        loadPage();
                }
        }

        @FXML
        private void handleNext() {
                if (currentPageNumber + 1 < pageTokens.size()) {
                        currentPageNumber++;
                        loadPage();
                }
        }

        private void loadPage() {
                String token = pageTokens.get(currentPageNumber);

                PageRequest pageRequest = PageRequest.builder()
                                .pageSize(PAGE_SIZE)
                                .pageToken(token)
                                .build();

                Page<FinancialTransaction> page = transactionHistoryService.getExpenses(pageRequest);

                transactionsTable.setItems(
                                FXCollections.observableArrayList(page.getContent()));

                pageLabel.setText("Page " + (currentPageNumber + 1));

                previousButton.setDisable(currentPageNumber == 0);
                nextButton.setDisable(!page.isHasNext());

                if (page.isHasNext()) {
                        String nextToken = page.getNextPageToken();

                        if (pageTokens.size() == currentPageNumber + 1) {
                                pageTokens.add(nextToken);
                        } else {
                                pageTokens.set(currentPageNumber + 1, nextToken);
                                pageTokens.subList(
                                                currentPageNumber + 2,
                                                pageTokens.size()).clear();
                        }
                } else {
                        pageTokens.subList(
                                        currentPageNumber + 1,
                                        pageTokens.size()).clear();
                }
        }
}
