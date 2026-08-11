package org.usf.inspect.server.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.Setter;
import org.usf.inspect.core.DirectoryRequestSignal;
import org.usf.inspect.core.DirectoryRequestUpdate;

/**
 * Represents a directory service request and its conversion to core request DTOs.
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

	/**
	 * Creates an empty directory request.
	 */
	@JsonCreator public DirectoryRequest() { }

    /**
     * Converts this directory request to its core request signal representation.
     *
     * @return the request signal built from this directory request
     */
    public DirectoryRequestSignal toRequest() {
        DirectoryRequestSignal dr = new DirectoryRequestSignal(getId(), getSessionId(), getStart(), getThreadName());
        dr.setInstanceId(getInstanceId());
        dr.setUser(getUser());
        dr.setProtocol(getProtocol());
        dr.setHost(getHost());
        dr.setPort(getPort());
        return dr;
    }

    /**
     * Converts this directory request to its callback update representation.
     *
     * @return the callback update built from this directory request
     */
    public DirectoryRequestUpdate toCallback() {
        DirectoryRequestUpdate drc = new DirectoryRequestUpdate(getId());
        drc.setEnd(getEnd());
        drc.setFailed(isFailed());
        drc.setCommand(getCommand());
        return drc;
    }
}
