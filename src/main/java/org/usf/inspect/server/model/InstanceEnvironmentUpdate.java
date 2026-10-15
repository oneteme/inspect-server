package org.usf.inspect.server.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.util.UUID;

import org.usf.inspect.EventTrace;

@Getter
@RequiredArgsConstructor
public final class InstanceEnvironmentUpdate implements EventTrace {
	
    private final UUID id;
    private final Instant end;
}
