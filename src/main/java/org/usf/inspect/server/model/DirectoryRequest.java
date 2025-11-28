package org.usf.inspect.server.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.Setter;
import org.usf.inspect.core.DirectoryRequest2;
import org.usf.inspect.core.DirectoryRequestCallback;

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

    public DirectoryRequest2 toRequest() {
        DirectoryRequest2 dr = new DirectoryRequest2(getId(), getSessionId(), getStart(), getThreadName());
        dr.setInstanceId(getInstanceId());
        dr.setUser(getUser());
        dr.setProtocol(getProtocol());
        dr.setHost(getHost());
        dr.setPort(getPort());
        return dr;
    }

    public DirectoryRequestCallback toCallback() {
        DirectoryRequestCallback drc = new DirectoryRequestCallback(getId());
        drc.setEnd(getEnd());
        drc.setFailed(isFailed());
        drc.setCommand(getCommand());
        return drc;
    }
}
