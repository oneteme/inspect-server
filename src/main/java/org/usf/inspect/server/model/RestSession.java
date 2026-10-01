package org.usf.inspect.server.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Delegate;
import org.usf.inspect.core.ExceptionTrace;
import org.usf.inspect.core.HttpSessionSignal;
import org.usf.inspect.core.HttpSessionUpdate;

/**
 * 
 * @author u$f
 *
 */
@Getter
@Setter
public class RestSession extends AbstractSession {

	@JsonIgnore
	@Delegate
	private final RestRequest rest;
	private String name; //api name
	private String userAgent; //Mozilla, Chrome, curl, Postman,..
	private String cacheControl; //max-age, no-cache

	@JsonCreator public RestSession() {
		this.rest = new RestRequest();
	}
}
