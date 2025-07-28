package org.usf.inspect.server.model.wrapper;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Delegate;
import org.usf.inspect.core.LogEntry;
import org.usf.inspect.core.StackTraceRow;
import org.usf.inspect.server.model.InstanceEventTrace;

import java.time.Instant;

import static org.usf.inspect.core.LogEntry.*;

@Getter
@Setter
public class LogEntryWrapper extends InstanceEventTrace {

    @Delegate
    private final LogEntry logEntry;

    public LogEntryWrapper(Instant instant, Level level, String message, StackTraceRow[] stackRows) {
        this.logEntry = new LogEntry(instant, level, message, stackRows);
    }
}
