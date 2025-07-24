package org.usf.inspect.server.dto;

import java.time.Instant;
import org.usf.inspect.server.model.ExceptionInfo;
import org.usf.inspect.server.model.AbstractRequest;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class DtoRequest extends AbstractRequest {
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
    protected ExceptionInfo exception;
}
