package org.usf.inspect.server.model.wrapper;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.usf.inspect.core.LocalRequest;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Delegate;

@Getter
@Setter
public class LocalRequestWrapper extends LocalRequest {

    private long id;
    private boolean status;

    private final String cdSession;
    @JsonIgnore
    @Delegate
    private final LocalRequest stage;

    public LocalRequestWrapper(String parentId){
        this.cdSession = parentId;
        this.stage = new LocalRequest();
    }

    public LocalRequestWrapper(String parentId, LocalRequest stage) {
        this.cdSession = parentId;
        this.stage = stage;
    }
}
