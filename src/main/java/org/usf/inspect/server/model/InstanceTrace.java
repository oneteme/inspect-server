package org.usf.inspect.server.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.usf.inspect.core.EventTrace;

import java.time.Instant;

@Getter
@RequiredArgsConstructor
public class InstanceTrace implements EventTrace {
	
    private final Integer pending;
    private final Integer attempts;
    private final int traceCount;
    private final String fileName;
    private final Instant instant;
    private final String instanceId;
}
