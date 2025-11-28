package org.usf.inspect.server.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Delegate;
import org.usf.inspect.core.MainSession2;
import org.usf.inspect.core.MainSessionCallback;

/**
 * 
 * @author u$f
 *
 */
@Getter
@Setter
public class MainSession extends AbstractSession {

	@JsonIgnore
	@Delegate()
	private final LocalRequest local; //!exception

	public MainSession() {
		this.local = new LocalRequest();
	}

    public MainSession2 toSession() {
        var session = new MainSession2(getId(), getStart(), getThreadName(), getType());
        session.setLocation(getLocation());
        session.setUser(getUser());
        session.setName(getName());
        session.setInstanceId(getInstanceId());
        return session;
    }

    public MainSessionCallback toCallback() {
        var callback = new MainSessionCallback(getId());
        callback.setStart(getStart());
        callback.setEnd(getEnd());
        callback.setLocation(getLocation());
        callback.setUser(getUser());
        callback.setName(getName());
        callback.setRequestMask(getRequestsMask());
        callback.setException(getException());
        return callback;
    }
}
