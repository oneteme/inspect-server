package org.usf.inspect.server.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LocalRequest extends SessionStage {
    private String name;
    private String location;
    private ExceptionInfo exception;

    private boolean status;
}
