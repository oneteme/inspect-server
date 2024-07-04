package org.usf.inspect.server.model.wrapper;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.usf.inspect.core.ExceptionInfo;

@Getter
@Setter
@RequiredArgsConstructor
public class ExceptionWrapper {
    private final long cdRequest;
    private final Long order;
    private final ExceptionInfo exceptionInfo;
}
