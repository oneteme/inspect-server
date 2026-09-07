package org.usf.inspect.server.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.usf.inspect.core.*;

import java.time.Instant;
import java.util.UUID;

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
	private UUID sessionId;
	private UUID instanceId;

	@Deprecated(forRemoval = true)
    public abstract TraceSignal toRequest();

	@Deprecated(forRemoval = true)
    public abstract TraceUpdate toCallback();
}