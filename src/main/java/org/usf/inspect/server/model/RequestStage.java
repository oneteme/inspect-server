package org.usf.inspect.server.model;

import java.time.Instant;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RequestStage implements Metric {
    private String name;
    private Instant start;
    private Instant end;
    private ExceptionInfo exception;

    private String idRequest;
    private Integer order;
}
