package org.usf.inspect.server.model;

import org.usf.inspect.core.RestRequest;
import org.usf.inspect.core.RestSession;

import lombok.Getter;
import lombok.Setter;
import org.usf.inspect.server.model.wrapper.RestRequestWrapper;

import java.util.function.Supplier;

/**
 * 
 * @author u$f
 *
 */
@Getter
@Setter
public  class Exchange extends RestRequest{
	
	private RestSession remoteTrace;

}
