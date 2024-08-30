package org.usf.inspect.server.model.wrapper;


import org.usf.inspect.core.DatabaseRequest;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Delegate;

@Setter
@Getter
public class DatabaseRequestWrapper extends DatabaseRequest {

    private long id;
    private boolean completed;

    private final String cdSession;

    @JsonIgnore
    @Delegate
    private final DatabaseRequest databaseRequest;


    public DatabaseRequestWrapper(String cdSession) {
        this.cdSession = cdSession;
        this.databaseRequest = new DatabaseRequest();
    }

    public DatabaseRequestWrapper(String cdSession, DatabaseRequest databaseRequest) {
        this.cdSession = cdSession;
        this.databaseRequest = databaseRequest;
    }
}
