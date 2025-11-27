package org.usf.inspect.server.model.wrapper;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Delegate;
import org.usf.inspect.core.CommandType;
import org.usf.inspect.core.DatabaseCommand;
import org.usf.inspect.core.DatabaseRequestStage;
import org.usf.inspect.core.EventTrace;
import org.usf.inspect.server.model.DatabaseRequest;

import java.util.List;
import java.util.Optional;

import static java.util.Objects.nonNull;

@Getter
@Setter
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, defaultImpl = DatabaseRequestWrapper.class)
@Deprecated(since = "v1.1")
public final class DatabaseRequestWrapper implements EventTrace, Wrapper<DatabaseRequest> {

    @Delegate
    @JsonIgnore
    private final DatabaseRequest request = new DatabaseRequest();

    private List<DatabaseRequestStage> actions;

    public String mainCommand(){
        DatabaseCommand main = null;
        for(DatabaseRequestStage stage : actions){
            DatabaseCommand c = enumOf(stage);
            if(nonNull(c)){
                main = DatabaseCommand.mergeCommand(main, c);
            }
        }
        return Optional.ofNullable(main).map(DatabaseCommand::getType).map(CommandType::name).orElse(null);
    }

    private static DatabaseCommand enumOf(DatabaseRequestStage stage) {
        try {
            return DatabaseCommand.valueOf(stage.getName());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Override
    public DatabaseRequest unwrap() {
        return request;
    }
}
