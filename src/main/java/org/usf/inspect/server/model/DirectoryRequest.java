package org.usf.inspect.server.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.Setter;
import org.usf.inspect.core.DirectoryRequestSignal;
import org.usf.inspect.core.DirectoryRequestUpdate;

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

    public DirectoryRequestSignal toRequest() {
        DirectoryRequestSignal dr = new DirectoryRequestSignal(getId(), getSessionId(), getStart(), getThreadName());
        dr.setInstanceId(getInstanceId());
        dr.setUser(getUser());
        dr.setProtocol(getProtocol());
        dr.setHost(getHost());
        dr.setPort(getPort());
        return dr;
    }

    public DirectoryRequestUpdate toCallback() {
        DirectoryRequestUpdate drc = new DirectoryRequestUpdate(getId());
        drc.setEnd(getEnd());
        drc.setFailed(isFailed());
        drc.setCommand(getCommand());
        return drc;
    }
}
