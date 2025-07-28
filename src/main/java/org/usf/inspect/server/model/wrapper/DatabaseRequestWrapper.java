package org.usf.inspect.server.model.wrapper;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Delegate;
import org.usf.inspect.core.DatabaseRequestStage;
import org.usf.inspect.jdbc.SqlCommand;
import org.usf.inspect.core.DatabaseRequest;
import org.usf.inspect.server.model.InstanceEventTrace;
import org.usf.inspect.server.model.Wrapper;

import java.util.*;
import java.util.stream.Collectors;

@Getter
@Setter
public final class DatabaseRequestWrapper extends InstanceEventTrace implements Wrapper<DatabaseRequest> {

    @Delegate
    @JsonIgnore
    private final DatabaseRequest request = new DatabaseRequest();

    @Deprecated(since = "v1.1", forRemoval = true)
    private List<DatabaseRequestStage> actions;

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

    @Override
    public DatabaseRequest unwrap() {
        return request;
    }
}
