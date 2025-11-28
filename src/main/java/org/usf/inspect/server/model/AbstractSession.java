package org.usf.inspect.server.model;

import lombok.Getter;
import lombok.Setter;
import org.usf.inspect.core.EventTrace;

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
