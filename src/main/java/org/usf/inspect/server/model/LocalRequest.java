package org.usf.inspect.server.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.Setter;
import org.usf.inspect.core.ExceptionInfo;
import org.usf.inspect.core.LocalRequest2;
import org.usf.inspect.core.LocalRequestCallback;

/**
 * 
 * @author u$f
 *
 */
@Getter
@Setter
public class LocalRequest extends AbstractRequest {

	private String name; //title, topic
	private String type;
	private String location; //class.method, URL
	private ExceptionInfo exception;
	
	@JsonCreator public LocalRequest() { }

    public LocalRequest2 toRequest() {
        var req = new LocalRequest2(getId(), getSessionId(), getStart(), getThreadName());
        req.setLocation(getLocation());
        req.setUser(getUser());
        req.setName(getName());
        req.setType(getType());
        req.setInstanceId(getInstanceId());
        return req;
    }

    public LocalRequestCallback toCallback() {
        var callback = new LocalRequestCallback(getId());
        callback.setEnd(getEnd());
        callback.setCommand(getCommand());
        callback.setException(getException());
        return callback;
    }
}