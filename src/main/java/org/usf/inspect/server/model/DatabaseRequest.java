package org.usf.inspect.server.model;

import static org.usf.inspect.server.Utils.isEmpty;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.usf.inspect.jdbc.SqlCommand;

import lombok.Getter;
import lombok.Setter;

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
    private String command;
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

    public void updateIdRequest() {
        if(!isEmpty(getActions())) {
            var inc = new AtomicInteger(0);
            for(DatabaseRequestStage stage: getActions()) {
                stage.setIdRequest(getIdRequest());
                stage.setOrder(inc.incrementAndGet());
            }
        }
    }
}
