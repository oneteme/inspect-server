package org.usf.inspect.server.model.wrapper;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Delegate;
import org.usf.inspect.core.EventTrace;
import org.usf.inspect.core.HttpSessionStage;
import org.usf.inspect.server.model.Wrapper;

@Getter
@Setter
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, defaultImpl = HttpSessionStageWrapper.class)
public class HttpSessionStageWrapper implements EventTrace, Wrapper<HttpSessionStage> {
    @Delegate
    @JsonIgnore
    private HttpSessionStage stage = new HttpSessionStage();

    @Override
    public HttpSessionStage unwrap() {
        return stage;
    }
}
