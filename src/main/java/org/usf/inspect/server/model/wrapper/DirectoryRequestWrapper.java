package org.usf.inspect.server.model.wrapper;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Delegate;
import org.usf.inspect.core.DirectoryRequest;
import org.usf.inspect.core.DirectoryRequestStage;
import org.usf.inspect.core.EventTrace;

import java.util.List;

@Getter
@Setter
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, defaultImpl = DirectoryRequestWrapper.class)
@Deprecated(since = "v1.1")
public class DirectoryRequestWrapper implements EventTrace, Wrapper<DirectoryRequest> {
    @Delegate
    @JsonIgnore
    private final DirectoryRequest request = new DirectoryRequest();

    private List<DirectoryRequestStage> actions;

    @Override
    public DirectoryRequest unwrap() {
        return request;
    }
}
