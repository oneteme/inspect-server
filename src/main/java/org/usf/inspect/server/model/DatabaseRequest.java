package org.usf.inspect.server.model;

import lombok.Getter;
import lombok.Setter;
import org.usf.inspect.jdbc.SqlCommand;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.usf.inspect.server.Utils.isEmpty;

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
    @Deprecated(since = "v1.1", forRemoval = true)
    private List<DatabaseRequestStage> actions;
    private String command;



    @Deprecated(since = "v1.1", forRemoval = true)
    private boolean status;

    private boolean failed;
    public void setStatus(boolean status) {
        failed = status;
    }

    public String mainCommand(){
        Set<SqlCommand> r = Optional.ofNullable(actions).orElseGet(Collections::emptyList).stream().map(DatabaseRequestStage::getCommands).filter(Objects::nonNull).flatMap(Arrays::stream).collect(Collectors.toSet());
        if(r.size() == 1) {
            var s = r.iterator().next();
            if (s != null) {
                return s.toString();
            }
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
