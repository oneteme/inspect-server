package org.usf.inspect.server.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.Setter;
import org.usf.inspect.core.MailRequestSignal;
import org.usf.inspect.core.MailRequestUpdate;

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

	public MailRequestSignal toRequest() {
        MailRequestSignal req = new MailRequestSignal(getId(), getSessionId(), getStart(), getThreadName());
        req.setInstanceId(getInstanceId());
        req.setUser(getUser());
        req.setProtocol(getProtocol());
        req.setHost(getHost());
        req.setPort(getPort());
        return req;
    }

    public MailRequestUpdate toCallback() {
        MailRequestUpdate cb = new MailRequestUpdate(getId());
        cb.setEnd(getEnd());
        cb.setCommand(getCommand());
        cb.setFailed(isFailed());
        return cb;
    }
}
