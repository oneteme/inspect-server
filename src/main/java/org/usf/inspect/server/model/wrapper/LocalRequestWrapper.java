package org.usf.inspect.server.model.wrapper;

import org.usf.inspect.core.LocalRequest;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Delegate;

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
