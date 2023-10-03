package org.usf.trace.api.server;

import lombok.Setter;
import org.usf.traceapi.core.ApiSession;
import org.usf.traceapi.core.ApiRequest;

import lombok.Getter;

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
