package org.usf.inspect.server.model;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public abstract class RequestStage {
    private String name;
    private Instant start;
    private Instant end;
    private ExceptionInfo exception;
}
