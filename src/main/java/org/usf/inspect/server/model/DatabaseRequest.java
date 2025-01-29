package org.usf.inspect.server.model;

import lombok.Getter;
import lombok.Setter;
import org.usf.inspect.jdbc.SqlCommand;

import java.util.*;
import java.util.stream.Collectors;

@Getter
@Setter
public class DatabaseRequest extends SessionStage {
    private String host;
    private Integer port;
    private String name;
    private String schema;
    private String driverVersion;
    private String productName;
    private String productVersion;
    private List<DatabaseRequestStage> actions;
    private String commands;
    private boolean status;

    public String mainCommand(){
        Set<SqlCommand> r = Optional.ofNullable(actions).orElseGet(Collections::emptyList).stream().map(DatabaseRequestStage::getCommands).filter(Objects::nonNull).flatMap(Arrays::stream).collect(Collectors.toSet());
        if(r.size() == 1){
            return r.iterator().next().toString();
        }
        if(r.size() > 1){
            return "SQL";
        }
        return null;
    }
}
