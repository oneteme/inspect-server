package org.usf.inspect.server.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@RequiredArgsConstructor
public class InstanceTrace {
    private final Integer pending;
    private final int attempts;
    private final int sessionLength;
    private final Instant instant;
    private final String  instanceId;
}
