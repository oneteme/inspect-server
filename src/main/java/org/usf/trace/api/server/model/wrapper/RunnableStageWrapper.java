package org.usf.trace.api.server.model.wrapper;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Delegate;
import org.usf.traceapi.core.LocalRequest;
import org.usf.traceapi.core.SessionStage;

@Getter
@Setter
public class RunnableStageWrapper {
    @Delegate
    private final LocalRequest stage;
    private final String parentId;

    public RunnableStageWrapper(String parentId){
        this.parentId = parentId;
        this.stage = new LocalRequest();
    }

    public RunnableStageWrapper(String parentId, LocalRequest stage) {
        this.parentId = parentId;
        this.stage = stage;
    }
}
