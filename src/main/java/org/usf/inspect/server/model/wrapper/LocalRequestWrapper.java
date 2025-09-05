package org.usf.inspect.server.model.wrapper;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Delegate;
import org.usf.inspect.core.LocalRequest;

@Getter
@Setter
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, defaultImpl = LocalRequestWrapper.class)
@Deprecated(since = "v1.1")
public class LocalRequestWrapper implements Wrapper<LocalRequest> {
    @Delegate
    @JsonIgnore
    private final LocalRequest request = new LocalRequest();

    private boolean failed;

    @Override
    public LocalRequest unwrap() {
        return request;
    }
}
