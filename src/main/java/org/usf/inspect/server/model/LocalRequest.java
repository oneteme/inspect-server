package org.usf.inspect.server.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.Setter;
import org.usf.inspect.core.ExceptionInfo;
import org.usf.inspect.core.LocalRequestSignal;
import org.usf.inspect.core.LocalRequestUpdate;

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

    public LocalRequestSignal toRequest() {
        var req = new LocalRequestSignal(getId(), getSessionId(), getStart(), getThreadName());
        req.setLocation(getLocation());
        req.setUser(getUser());
        req.setName(getName());
        req.setType(getType());
        req.setInstanceId(getInstanceId());
        return req;
    }

    public LocalRequestUpdate toCallback() {
        var callback = new LocalRequestUpdate(getId());
        callback.setEnd(getEnd());
        callback.setCommand(getCommand());
        callback.setException(getException());
        return callback;
    }
}