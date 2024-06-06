package org.usf.trace.api.server.model.wrapper;

import lombok.Getter;
import lombok.experimental.Delegate;
import org.usf.traceapi.core.DatabaseRequestStage;
import org.usf.traceapi.core.ExceptionInfo;
import org.usf.traceapi.jdbc.JDBCAction;

import java.time.Instant;

@Getter
public class DatabaseActionWrapper {
    @Delegate
    private final DatabaseRequestStage action;
    private final long parentId;

    public DatabaseActionWrapper(long parentId, JDBCAction type, Instant start, Instant end, ExceptionInfo exception, long[] count) {
        this.parentId = parentId;
        this.action = new DatabaseRequestStage(type, start, end, exception, count);
    }
}
