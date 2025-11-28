package org.usf.inspect.server.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.usf.inspect.core.*;

import java.time.Instant;

/**
 * 
 * @author u$f
 *
 */
@Getter
@Setter
@NoArgsConstructor
public abstract class AbstractRequest implements EventTrace {
	
	private Instant start;
	private Instant end;
	private String threadName;
	private String user;
	private String id;
	private String command;
	private String sessionId;
	private String instanceId;
	
	AbstractRequest(AbstractRequest req) {
		this.start = req.start;
		this.end = req.end;
		this.threadName = req.threadName;
		this.user = req.user;
		this.id = req.id;
		this.command = req.command;
		this.sessionId = req.sessionId;
		this.instanceId = req.instanceId;
	}


    public abstract Initializer toRequest();

    public abstract Callback toCallback();
}