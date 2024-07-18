package org.usf.inspect.server.model.wrapper;


import org.usf.inspect.core.DatabaseRequest;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Delegate;

@Setter
@Getter
public class DatabaseRequestWrapper {

    @JsonIgnore
    private final String parentId;
    private long idRequest;
    @JsonIgnore
    @Delegate
    private final DatabaseRequest databaseRequest;
    private boolean completed;

    public DatabaseRequestWrapper(String parentId) {
        this.parentId = parentId;
        this.databaseRequest = new DatabaseRequest();
    }

    public DatabaseRequestWrapper(String parentId, DatabaseRequest databaseRequest) {
        this.parentId = parentId;
        this.databaseRequest = databaseRequest;
    }
}
