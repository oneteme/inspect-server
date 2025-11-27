package org.usf.inspect.server.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.Setter;

/**
 * 
 * @author u$f
 *
 */
@Setter
@Getter
public class MailRequest extends AbstractRequest {

	private String protocol; //smtp(s), imap, pop3
	private String host;
	private int port;
	private boolean failed;

	@JsonCreator public MailRequest() { }

	MailRequest(MailRequest req) {
		super(req);
		this.protocol = req.protocol;
		this.host = req.host;
		this.port = req.port;
		this.failed = req.failed;
	}
}
