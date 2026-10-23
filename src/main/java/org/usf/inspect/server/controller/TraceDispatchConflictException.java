package org.usf.inspect.server.controller;

import org.usf.inspect.core.TraceDispatchException;

/**
 * 
 * @author u$f
 *
 */
@SuppressWarnings("serial")
public class TraceDispatchConflictException extends TraceDispatchException {

	public TraceDispatchConflictException(String message) {
		super(message);
	}
}
