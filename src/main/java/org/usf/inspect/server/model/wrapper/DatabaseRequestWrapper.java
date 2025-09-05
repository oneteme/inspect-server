package org.usf.inspect.server.model.wrapper;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Delegate;
import org.usf.inspect.core.DatabaseCommand;
import org.usf.inspect.core.DatabaseRequest;
import org.usf.inspect.core.DatabaseRequestStage;
import org.usf.inspect.core.EventTrace;

import java.util.List;

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
        DatabaseCommand c = actions.stream().map(DatabaseRequestWrapper::enumOf).reduce(DatabaseCommand::mergeCommand).orElse(null);
        return nonNull(c) ? c.getType().name() : null;
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
