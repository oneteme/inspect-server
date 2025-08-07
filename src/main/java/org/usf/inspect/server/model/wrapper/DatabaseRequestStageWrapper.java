package org.usf.inspect.server.model.wrapper;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Delegate;
import org.usf.inspect.core.DatabaseRequestStage;
import org.usf.inspect.core.EventTrace;
import org.usf.inspect.server.model.Wrapper;

@Getter
@Setter
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, defaultImpl = DatabaseRequestStageWrapper.class)
@Deprecated(since = "v1.1")
public class DatabaseRequestStageWrapper implements EventTrace, Wrapper<DatabaseRequestStage> {
    @Delegate
    @JsonIgnore
    private final DatabaseRequestStage stage = new DatabaseRequestStage();

    @Override
    public DatabaseRequestStage unwrap() {
        return stage;
    }
}
