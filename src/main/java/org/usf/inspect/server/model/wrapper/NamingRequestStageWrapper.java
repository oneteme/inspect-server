package org.usf.inspect.server.model.wrapper;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Delegate;
import org.usf.inspect.core.EventTrace;
import org.usf.inspect.core.NamingRequestStage;
import org.usf.inspect.server.model.Wrapper;

@Getter
@Setter
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, defaultImpl = NamingRequestStageWrapper.class)
public class NamingRequestStageWrapper implements EventTrace, Wrapper<NamingRequestStage> {
    @Delegate
    @JsonIgnore
    private NamingRequestStage stage = new NamingRequestStage();

    @Override
    public NamingRequestStage unwrap() {
        return stage;
    }
}
