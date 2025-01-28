package org.usf.inspect.server.model;

import lombok.Getter;
import lombok.Setter;
import org.usf.inspect.jdbc.SqlCommand;

@Getter
@Setter
public final class DatabaseRequestStage extends RequestStage {
    private long[] count;
    private SqlCommand[] commands;
}
