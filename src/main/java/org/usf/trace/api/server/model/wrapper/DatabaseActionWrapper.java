package org.usf.trace.api.server.model.wrapper;

import lombok.Getter;
import lombok.experimental.Delegate;
import org.usf.traceapi.core.DatabaseAction;
import org.usf.traceapi.core.ExceptionInfo;
import org.usf.traceapi.core.JDBCAction;

import java.time.Instant;

@Getter
public class DatabaseActionWrapper {
    @Delegate
    private final DatabaseAction action;
    private final long parentId;

    public DatabaseActionWrapper(long parentId, JDBCAction type, Instant start, Instant end, ExceptionInfo exception, long[] count) {
        this.parentId = parentId;
        this.action = new DatabaseAction(type, start, end, exception, count);
    }
}
