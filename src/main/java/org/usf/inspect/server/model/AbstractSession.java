package org.usf.inspect.server.model;

import lombok.Getter;
import lombok.Setter;

/**
 * 
 * @author u$f
 *
 */
@Getter
@Setter
public abstract class AbstractSession {

	private int requestsMask;

	AbstractSession() {
	}

	AbstractSession(AbstractSession req) {
		this.requestsMask = req.requestsMask;
	}
}
