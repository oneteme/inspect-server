package org.usf.trace.api.server.model.wrapper;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Delegate;
import org.usf.traceapi.core.RunnableStage;
import org.usf.traceapi.core.SessionStage;

@Getter
@Setter
public class RunnableStageWrapper {
    @Delegate
    private final RunnableStage stage;
    private final String parentId;

    public RunnableStageWrapper(String parentId){
        this.parentId = parentId;
        this.stage = new RunnableStage();
    }

    public RunnableStageWrapper(String parentId, RunnableStage stage) {
        this.parentId = parentId;
        this.stage = stage;
    }
}
