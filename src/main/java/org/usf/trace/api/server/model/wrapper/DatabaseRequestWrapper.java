package org.usf.trace.api.server.model.wrapper;


import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Delegate;
import org.usf.traceapi.core.DatabaseRequest;
import org.usf.traceapi.core.MailRequest;

@Setter
@Getter
public class DatabaseRequestWrapper {

    @JsonIgnore
    private final String parentId;
    @Delegate
    private final DatabaseRequest databaseRequest;
    private long id;
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
