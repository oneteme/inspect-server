package org.usf.inspect.server.model.wrapper;

import java.util.List;

import org.usf.inspect.DatabaseRequestStage;
import org.usf.inspect.EventTrace;
import org.usf.inspect.server.model.DatabaseRequest;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Delegate;

@Getter
@Setter
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, defaultImpl = DatabaseRequestWrapper.class)
/**
 * @deprecated
 */
@Deprecated(since = "v1.1")
public final class DatabaseRequestWrapper implements EventTrace {

    @Delegate
    @JsonIgnore
    private final DatabaseRequest request = new DatabaseRequest();

    private List<DatabaseRequestStage> actions;
}
