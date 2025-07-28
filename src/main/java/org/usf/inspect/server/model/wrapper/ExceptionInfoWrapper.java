package org.usf.inspect.server.model.wrapper;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Delegate;
import org.usf.inspect.core.ExceptionInfo;
import org.usf.inspect.core.StackTraceRow;

@Getter
@Setter
public class ExceptionInfoWrapper {
    @Delegate
    @JsonIgnore
    private final ExceptionInfo exceptionInfo;

    private String requestId;
    private Integer order;

    public ExceptionInfoWrapper(String type, String message, StackTraceRow[] stackTraceRows, ExceptionInfo cause) {
        this.exceptionInfo = new ExceptionInfo(type, message, stackTraceRows, cause);
    }
}
