package org.usf.inspect.server.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.usf.inspect.core.EventTrace;

import java.time.Instant;

/**
 * Represents an environment update event for a traced instance.
 */
@Getter
@RequiredArgsConstructor
public final class InstanceEnvironmentUpdate implements EventTrace {
	
    private final String id;
    private final Instant end;
}
