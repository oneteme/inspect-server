package org.usf.inspect.server.model;

import com.fasterxml.jackson.annotation.JsonTypeName;
import org.usf.inspect.jdbc.SqlCommand;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonTypeName("--")
public final class DatabaseRequestStage extends RequestStage {
    private long[] count;
    private SqlCommand[] commands;
}
