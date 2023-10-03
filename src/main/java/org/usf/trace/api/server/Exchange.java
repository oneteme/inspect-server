package org.usf.trace.api.server;

import lombok.Setter;
import org.usf.traceapi.core.IncomingRequest;
import org.usf.traceapi.core.OutcomingRequest;

import lombok.Getter;

/**
 * 
 * @author u$f
 *
 */
@Getter
@Setter
public final class Exchange extends OutcomingRequest {
	
	private IncomingRequest remoteTrace;

	public Exchange(String id) {
		super(id);
	}
	
}
