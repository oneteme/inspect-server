package org.usf.inspect.server.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.Setter;
import org.usf.inspect.core.ExceptionTrace;
import org.usf.inspect.core.LocalRequestSignal;
import org.usf.inspect.core.LocalRequestUpdate;

/**
 * 
 * @author u$f
 *
 */
@Getter
@Setter
public class LocalRequest extends AbstractRequest {

	private String name; //title, topic
	private String type;
	private String location; //class.method, URL
	
	@JsonCreator public LocalRequest() { }
}
