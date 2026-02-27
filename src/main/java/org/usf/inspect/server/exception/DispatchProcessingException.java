package org.usf.inspect.server.exception;

import lombok.Getter;

@Getter
@SuppressWarnings("serial")
public class DispatchProcessingException extends Exception {
    private final boolean retryable;

    public DispatchProcessingException(boolean retryable, Throwable cause) {
        super(cause);
        this.retryable = retryable;
    }
}
