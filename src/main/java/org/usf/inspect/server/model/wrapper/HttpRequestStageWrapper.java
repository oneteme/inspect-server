package org.usf.inspect.server.model.wrapper;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Delegate;
import org.usf.inspect.core.EventTrace;
import org.usf.inspect.core.HttpRequestStage;
import org.usf.inspect.server.model.Wrapper;

@Getter
@Setter
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, defaultImpl = HttpRequestStageWrapper.class)
public class HttpRequestStageWrapper implements EventTrace, Wrapper<HttpRequestStage> {
    @Delegate
    @JsonIgnore
    private HttpRequestStage stage = new HttpRequestStage();

    @Override
    public HttpRequestStage unwrap() {
        return stage;
    }
}
