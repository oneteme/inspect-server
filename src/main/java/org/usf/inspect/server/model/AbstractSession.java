package org.usf.inspect.server.model;

import org.usf.inspect.EventTrace;

import lombok.Getter;
import lombok.Setter;

/**
 * 
 * @author u$f
 *
 */
@Getter
@Setter
public abstract class AbstractSession implements EventTrace {

	private int requestsMask;

	AbstractSession() {
	}
}
