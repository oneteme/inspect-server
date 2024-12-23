package org.usf.inspect.server.model;

import lombok.Getter;
import lombok.Setter;
import org.usf.inspect.jdbc.SqlCommand;

import java.util.List;

@Getter
@Setter
public class DatabaseRequest extends SessionStage {
    private String host;
    private int port;
    private String name;
    private String schema;
    private String driverVersion;
    private String productName;
    private String productVersion;
    private List<DatabaseRequestStage> actions;
    private List<SqlCommand> commands;

    private boolean status;
}
