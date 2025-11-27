package org.usf.inspect.server.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Delegate;
import org.usf.inspect.core.*;

import java.time.Instant;

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
	private ExceptionInfo exception;

	@JsonCreator public RestSession() {
		this.rest = new RestRequest();
	}

	RestSession(RestSession ses) {
		super(ses);
		this.rest = new RestRequest(ses.rest);
		this.name = ses.name;
		this.userAgent = ses.userAgent;
		this.cacheControl = ses.cacheControl;
		this.exception = ses.exception;
	}
}