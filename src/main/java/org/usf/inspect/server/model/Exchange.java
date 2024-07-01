package org.usf.inspect.server.model;

import org.usf.inspect.core.RestRequest;
import org.usf.inspect.core.RestSession;

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
