package org.usf.trace.api.server.model.wrapper;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Delegate;
import org.usf.traceapi.core.DatabaseRequest;

@Setter
@Getter
public class DatabaseRequestWrapper {

    @Delegate
    private final DatabaseRequest query;
    private final String parentId;
    private long id;

    public DatabaseRequestWrapper(String parentId, Long id) {
        this.parentId = parentId;
        this.id = id;
        this.query = new DatabaseRequest();
    }

    public DatabaseRequestWrapper(String parentId, DatabaseRequest query) {
        this.parentId = parentId;
        this.query = query; //delegated getters
    }
}
