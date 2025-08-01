package org.usf.inspect.server.model;

import org.usf.inspect.jdbc.SqlCommand;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public final class DatabaseRequestStage extends RequestStage {
    private long[] count;
    private SqlCommand[] commands;
}
