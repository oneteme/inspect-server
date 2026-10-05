package org.usf.inspect.server.exception;

import org.usf.inspect.core.DispatchException;

import lombok.Getter;

/**
 * 
 * @author u$f
 *
 */
@Getter
@SuppressWarnings("serial")
public class DispatchProcessingException extends DispatchException {
    
	private final boolean retryable;

	public DispatchProcessingException(String message, boolean retryable) {
		this(message, null, retryable);
	}

    public DispatchProcessingException(String message, Throwable cause, boolean retryable) {
        super(message, cause);
        this.retryable = retryable;
    }
}
