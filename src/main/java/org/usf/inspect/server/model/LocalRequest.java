package org.usf.inspect.server.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.Setter;
import org.usf.inspect.core.ExceptionInfo;

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
	private ExceptionInfo exception;
	
	@JsonCreator public LocalRequest() { }

	LocalRequest(LocalRequest req) {
		super(req);
		this.name = req.name;
		this.type = req.type;
		this.location = req.location;
		this.exception = req.exception;
	}
}