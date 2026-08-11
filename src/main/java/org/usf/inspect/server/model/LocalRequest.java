package org.usf.inspect.server.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.Setter;
import org.usf.inspect.core.ExceptionInfo;
import org.usf.inspect.core.LocalRequestSignal;
import org.usf.inspect.core.LocalRequestUpdate;

/**
 * Represents a local request and its conversion to core local request DTOs.
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
	
	/**
	 * Creates an empty local request.
	 */
	@JsonCreator public LocalRequest() { }

    /**
     * Converts this local request to its core request signal representation.
     *
     * @return the request signal built from this local request
     */
    public LocalRequestSignal toRequest() {
        var req = new LocalRequestSignal(getId(), getSessionId(), getStart(), getThreadName());
        req.setLocation(getLocation());
        req.setUser(getUser());
        req.setName(getName());
        req.setType(getType());
        req.setInstanceId(getInstanceId());
        return req;
    }

    /**
     * Converts this local request to its callback update representation.
     *
     * @return the callback update built from this local request
     */
    public LocalRequestUpdate toCallback() {
        var callback = new LocalRequestUpdate(getId());
        callback.setEnd(getEnd());
        callback.setCommand(getCommand());
        callback.setException(getException());
        return callback;
    }
}