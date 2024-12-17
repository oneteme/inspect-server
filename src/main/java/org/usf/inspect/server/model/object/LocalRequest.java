package org.usf.inspect.server.model.object;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LocalRequest extends SessionStage {
    private String name;
    private String location;
    private ExceptionInfo exception;

    private long idRequest;
    private String cdSession;
    private boolean status;
}
