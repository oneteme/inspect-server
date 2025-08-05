package org.usf.inspect.server.model.wrapper;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Delegate;
import org.usf.inspect.core.DirectoryRequestStage;
import org.usf.inspect.core.EventTrace;
import org.usf.inspect.server.model.Wrapper;

@Getter
@Setter
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, defaultImpl = DirectoryRequestStageWrapper.class)
public class DirectoryRequestStageWrapper implements EventTrace, Wrapper<DirectoryRequestStage> {
    @Delegate
    @JsonIgnore
    private DirectoryRequestStage stage = new DirectoryRequestStage();

    @Override
    public DirectoryRequestStage unwrap() {
        return stage;
    }
}
