package org.usf.inspect.server.model.wrapper;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.usf.inspect.core.LocalRequest;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Delegate;

@Getter
@Setter
public class LocalRequestWrapper {
    private final String parentId;
    private long idRequest;
    @JsonIgnore
    @Delegate
    private final LocalRequest stage;

    public LocalRequestWrapper(String parentId){
        this.parentId = parentId;
        this.stage = new LocalRequest();
    }

    public LocalRequestWrapper(String parentId, LocalRequest stage) {
        this.parentId = parentId;
        this.stage = stage;
    }
}
