package org.usf.inspect.server.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Delegate;
import org.usf.inspect.core.MainSessionSignal;
import org.usf.inspect.core.MainSessionUpdate;

/**
 * Represents a main session and exposes conversions to core session DTOs.
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

	/**
	 * Creates an empty main session backed by a local request instance.
	 */
	public MainSession() {
		this.local = new LocalRequest();
	}

    /**
     * Converts this main session to its core session signal representation.
     *
     * @return the session signal built from this main session
     */
    public MainSessionSignal toSession() {
        var session = new MainSessionSignal(getId(), getStart(), getThreadName(), getType());
        session.setLocation(getLocation());
        session.setUser(getUser());
        session.setName(getName());
        session.setInstanceId(getInstanceId());
        return session;
    }

    /**
     * Converts this main session to its callback update representation.
     *
     * @return the callback update built from this main session
     */
    public MainSessionUpdate toCallback() {
        var callback = new MainSessionUpdate(getId());
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
