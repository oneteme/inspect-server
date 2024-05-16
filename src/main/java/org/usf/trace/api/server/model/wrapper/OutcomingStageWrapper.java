package org.usf.trace.api.server.model.wrapper;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Delegate;
import org.usf.traceapi.core.RunnableStage;

@Getter
@Setter
public class OutcomingStageWrapper {
    @Delegate
    private final RunnableStage stage;
    private final String parentId;

    public OutcomingStageWrapper(String parentId){
        this.parentId = parentId;
        this.stage = new RunnableStage();
    }

    public OutcomingStageWrapper(String parentId, RunnableStage stage) {
        this.parentId = parentId;
        this.stage = stage;
    }
}
