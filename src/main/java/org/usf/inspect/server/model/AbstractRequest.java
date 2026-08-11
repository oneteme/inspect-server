package org.usf.inspect.server.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.usf.inspect.core.*;

import java.time.Instant;

/**
 * Provides shared request fields and conversion contracts for request trace models.
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

    public abstract TraceSignal toRequest();

    public abstract TraceUpdate toCallback();
}