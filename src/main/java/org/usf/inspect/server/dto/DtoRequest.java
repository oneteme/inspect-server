package org.usf.inspect.server.dto;

import lombok.Getter;
import lombok.Setter;
import org.usf.inspect.server.model.wrapper.ExceptionInfoWrapper;

import java.time.Instant;

@Setter
@Getter
public class DtoRequest {
    protected String id;
    protected String type;
    protected String sessionType;
    protected String host;
    protected Instant start;
    protected Instant end;
    protected String threadName;
    protected String parent;
    protected String name;
    protected String command;
    protected String schema;
    protected boolean failed;
    protected ExceptionInfoWrapper exception;
    protected String user;
    protected String sessionId;
}
