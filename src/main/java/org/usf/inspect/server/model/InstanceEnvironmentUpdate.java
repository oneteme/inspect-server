package org.usf.inspect.server.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.usf.inspect.core.EventTrace;

import java.time.Instant;
import java.util.UUID;

@Getter
@RequiredArgsConstructor
public final class InstanceEnvironmentUpdate implements EventTrace {
	
    private final UUID id;
    private final Instant end;
}
