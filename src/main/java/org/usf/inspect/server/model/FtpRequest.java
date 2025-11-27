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
public class FtpRequest extends AbstractRequest {

	private String protocol; //FTP, FTPS
	private String host;
	private int port;  // -1 otherwise
	private String serverVersion;
	private String clientVersion;
	private boolean failed;

	@JsonCreator() public FtpRequest() { }

	FtpRequest(FtpRequest req) {
		super(req);
		this.protocol = req.protocol;
		this.host = req.host;
		this.port = req.port;
		this.serverVersion = req.serverVersion;
		this.clientVersion = req.clientVersion;
		this.failed = req.failed;
	}
}
