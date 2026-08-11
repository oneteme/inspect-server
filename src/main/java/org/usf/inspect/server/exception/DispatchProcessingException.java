package org.usf.inspect.server.exception;

import lombok.Getter;

/**
 * Represents a checked exception raised while processing a dispatch operation.
 */
@Getter
@SuppressWarnings("serial")
public class DispatchProcessingException extends Exception {
    private final boolean retryable;

    /**
     * Creates a new dispatch processing exception with retry information and a root cause.
     *
     * @param retryable whether the failed processing can be retried safely
     * @param cause the underlying cause of the processing failure
     */
    public DispatchProcessingException(boolean retryable, Throwable cause) {
        super(cause);
        this.retryable = retryable;
    }
}
