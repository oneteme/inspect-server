package org.usf.inspect.server.exception;

import org.usf.inspect.core.TraceDispatchException;

import lombok.Getter;

/**
 * 
 * @author u$f
 *
 */
@Getter
@SuppressWarnings("serial")
public class TraceDispatchProcessingException extends TraceDispatchException {
    
	private final boolean retryable;

	public TraceDispatchProcessingException(String message, boolean retryable) {
		this(message, null, retryable);
	}

    public TraceDispatchProcessingException(String message, Throwable cause, boolean retryable) {
        super(message, cause);
        this.retryable = retryable;
    }
}
