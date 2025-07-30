package org.usf.inspect.server.model.wrapper;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Delegate;
import org.usf.inspect.core.NamingRequest;
import org.usf.inspect.core.NamingRequestStage;
import org.usf.inspect.server.model.InstanceEventTrace;
import org.usf.inspect.server.model.Wrapper;

import java.util.List;

@Getter
@Setter
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, defaultImpl = NamingRequestWrapper.class)
public class NamingRequestWrapper extends InstanceEventTrace implements Wrapper<NamingRequest> {
    @Delegate
    @JsonIgnore
    private final NamingRequest request = new NamingRequest();

    @Deprecated(since = "v1.1")
    private List<NamingRequestStageWrapper> actions;

    @Override
    public NamingRequest unwrap() {
        return request;
    }
}
