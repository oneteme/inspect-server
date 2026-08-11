package org.usf.inspect.server.model.wrapper;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Delegate;
import org.usf.inspect.core.DatabaseRequestStage;
import org.usf.inspect.core.EventTrace;
import org.usf.inspect.server.model.DatabaseRequest;

import java.util.List;

/**
 * Deprecated wrapper that exposes a database request and its recorded stages for legacy API payloads.
 *
 * @deprecated since v1.1.
 */
@Getter
@Setter
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, defaultImpl = DatabaseRequestWrapper.class)
@Deprecated(since = "v1.1")
public final class DatabaseRequestWrapper implements EventTrace {

    @Delegate
    @JsonIgnore
    private final DatabaseRequest request = new DatabaseRequest();

    private List<DatabaseRequestStage> actions;
}
