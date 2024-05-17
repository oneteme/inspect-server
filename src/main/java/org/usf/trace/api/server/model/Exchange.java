package org.usf.trace.api.server.model;

import org.usf.traceapi.core.ApiRequest;
import org.usf.traceapi.core.ApiSession;

import lombok.Getter;
import lombok.Setter;

/**
 * 
 * @author u$f
 *
 */
@Getter
@Setter
public final class Exchange extends ApiRequest {
	
	private ApiSession remoteTrace;
	
}
