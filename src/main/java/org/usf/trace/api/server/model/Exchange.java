package org.usf.trace.api.server.model;

import org.usf.traceapi.core.RestRequest;
import org.usf.traceapi.core.RestSession;

import lombok.Getter;
import lombok.Setter;

/**
 * 
 * @author u$f
 *
 */
@Getter
@Setter
public final class Exchange extends RestRequest {
	
	private RestSession remoteTrace;
	
}
