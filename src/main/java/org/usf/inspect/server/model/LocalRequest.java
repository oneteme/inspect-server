package org.usf.inspect.server.model;

import java.util.List;


import lombok.Getter;
import lombok.Setter;

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
