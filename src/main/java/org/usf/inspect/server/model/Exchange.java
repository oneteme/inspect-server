package org.usf.inspect.server.model;

import lombok.Getter;
import lombok.Setter;
import org.usf.inspect.server.model.object.RestRequest;
import org.usf.inspect.server.model.object.RestSession;

/**
 * 
 * @author u$f
 *
 */
@Getter
@Setter
public class Exchange extends RestRequest {
	
	private RestSession remoteTrace;

}
