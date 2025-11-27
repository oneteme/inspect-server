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
public class DatabaseRequest extends AbstractRequest {

	private String scheme;
	private String host;
	private int port;
	private String name;
	private String schema;
	private String driverVersion;
	private String productName;
	private String productVersion;
	private boolean failed;

	@JsonCreator public DatabaseRequest() { }

	DatabaseRequest(DatabaseRequest req) {
		super(req);
		this.scheme = req.scheme;
		this.host = req.host;
		this.port = req.port;
		this.name = req.name;
		this.schema = req.schema;
		this.driverVersion = req.driverVersion;
		this.productName = req.productName;
		this.productVersion = req.productVersion;
		this.failed = req.failed;
	}
}
