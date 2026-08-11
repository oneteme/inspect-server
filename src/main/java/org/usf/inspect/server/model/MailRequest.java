package org.usf.inspect.server.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.Setter;
import org.usf.inspect.core.MailRequestSignal;
import org.usf.inspect.core.MailRequestUpdate;

/**
 * Represents a mail request and its conversion to core mail request DTOs.
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

	/**
	 * Creates an empty mail request.
	 */
	@JsonCreator public MailRequest() {
        // empty
    }

	/**
	 * Converts this mail request to its core request signal representation.
	 *
	 * @return the request signal built from this mail request
	 */
	public MailRequestSignal toRequest() {
        MailRequestSignal req = new MailRequestSignal(getId(), getSessionId(), getStart(), getThreadName());
        req.setInstanceId(getInstanceId());
        req.setUser(getUser());
        req.setProtocol(getProtocol());
        req.setHost(getHost());
        req.setPort(getPort());
        return req;
    }

    /**
     * Converts this mail request to its callback update representation.
     *
     * @return the callback update built from this mail request
     */
    public MailRequestUpdate toCallback() {
        MailRequestUpdate cb = new MailRequestUpdate(getId());
        cb.setEnd(getEnd());
        cb.setCommand(getCommand());
        cb.setFailed(isFailed());
        return cb;
    }
}
