package org.usf.inspect.server.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.Setter;

/**
 * 
 * @author u$f
 *
 */
@Getter
@Setter
public class DirectoryRequest extends AbstractRequest {
	
	private String protocol;
	private String host;
	private int port;
	private boolean failed;

	@JsonCreator public DirectoryRequest() { }

	DirectoryRequest(DirectoryRequest req) {
		super(req);
		this.protocol = req.protocol;
		this.host = req.host;
		this.port = req.port;
		this.failed = req.failed;
	}
}
