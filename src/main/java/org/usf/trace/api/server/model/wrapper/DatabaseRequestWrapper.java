package org.usf.trace.api.server.model.wrapper;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Delegate;
import org.usf.traceapi.core.DatabaseRequest;

@Setter
@Getter
public class DatabaseRequestWrapper extends DatabaseRequest {

    @JsonIgnore
    private final String parentId;
    private long id;
    private boolean completed;

    public DatabaseRequestWrapper(String parentId, Long id) {
        this.parentId = parentId;
        this.id = id;
    }

    @Deprecated
    public DatabaseRequestWrapper(String parentId, DatabaseRequest query) {
        this.parentId = parentId;
        this.setHost(query.getHost());
        this.setPort(query.getPort());
        this.setDatabase(query.getDatabase());
        this.setDriverVersion(query.getDriverVersion());
        this.setDatabaseName(query.getDatabaseName());
        this.setDatabaseVersion(query.getDatabaseVersion());
        this.setActions(query.getActions());
        this.setCommands(query.getCommands());
        this.setName(query.getName());
        this.setLocation(query.getLocation());
        this.setStart(query.getStart());
        this.setEnd(query.getEnd());
        this.setUser(query.getUser());
        this.setThreadName(query.getThreadName());

    }

}
