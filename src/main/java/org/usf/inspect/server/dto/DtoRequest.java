package org.usf.inspect.server.dto;

import java.time.Instant;

import org.usf.inspect.server.model.ExceptionInfo;
import org.usf.inspect.server.model.SessionStage;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class DtoRequest extends SessionStage {
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
    protected boolean status;
    protected ExceptionInfo exception;
}
