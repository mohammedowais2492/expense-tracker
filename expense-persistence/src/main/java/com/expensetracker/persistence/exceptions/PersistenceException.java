
package com.expensetracker.persistence.exceptions;

/**
 * Represents an unexpected failure during a persistence operation.
 *
 * <p>
 * This exception wraps lower-level database exceptions, allowing
 * persistence implementations to expose unchecked failures without
 * leaking JDBC-specific checked exceptions through repository contracts.
 * </p>
 *
 * <p>
 * The original exception is retained as the cause to support
 * debugging and error reporting.
 * </p>
 */
public class PersistenceException extends RuntimeException {

    /**
     * Constructs a persistence exception with a descriptive message
     * and the underlying cause.
     *
     * @param message a description of the failed persistence operation
     * @param cause   the underlying exception that caused the failure
     */
    public PersistenceException(String message, Throwable cause) {
        super(message, cause);
    }
}
