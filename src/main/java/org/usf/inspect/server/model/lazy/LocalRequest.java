package org.usf.inspect.server.model.lazy;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class LocalRequest extends SessionStage {
    private String name;
    private String location;
    private String type;
    private ExceptionInfo exception;
    private List<ExceptionInfo> exceptions;
    private boolean status;

    public ExceptionInfo getException(){
        if(exceptions != null && !exceptions.isEmpty()){
            return exceptions.getLast();
        }
        return exception;
    }
}
