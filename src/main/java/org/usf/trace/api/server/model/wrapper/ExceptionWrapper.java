package org.usf.trace.api.server.model.wrapper;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.usf.traceapi.core.ExceptionInfo;

@Getter
@Setter
@RequiredArgsConstructor
public class ExceptionWrapper {
    private final long parentId;
    private final ExceptionInfo exceptionInfo;
    private final Long order;
}
