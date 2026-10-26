package org.usf.inspect.server.model;

import java.time.Instant;
import java.util.UUID;

import org.usf.inspect.EventTrace;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
	private UUID id;
	private String command;
	private short status; //2xx, 4xx, 5xx, 0 otherwise
	private UUID sessionId;
	private UUID instanceId;
}
