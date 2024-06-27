package org.usf.trace.api.server.model.wrapper;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Delegate;
import org.usf.traceapi.core.LocalRequest;

@Getter
@Setter
public class LocalRequestWrapper {
    @Delegate
    private final LocalRequest stage;
    private final String parentId;

    public LocalRequestWrapper(String parentId){
        this.parentId = parentId;
        this.stage = new LocalRequest();
    }

    public LocalRequestWrapper(String parentId, LocalRequest stage) {
        this.parentId = parentId;
        this.stage = stage;
    }
}
