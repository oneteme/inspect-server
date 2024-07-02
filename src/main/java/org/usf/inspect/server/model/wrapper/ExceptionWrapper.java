package org.usf.inspect.server.model.wrapper;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.usf.inspect.core.ExceptionInfo;

@Getter
@Setter
@RequiredArgsConstructor
public class ExceptionWrapper {
    private final long parentId;
    private final ExceptionInfo exceptionInfo;
    private final Long order;
}
