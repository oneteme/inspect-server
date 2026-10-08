package org.usf.inspect.server.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Delegate;
import org.usf.inspect.core.MainSessionSignal;
import org.usf.inspect.core.MainSessionUpdate;

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
}
