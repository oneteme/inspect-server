package org.usf.inspect.server.model.wrapper;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Delegate;
import org.usf.inspect.core.EventTrace;
import org.usf.inspect.core.ExceptionInfo;
import org.usf.inspect.server.model.RestRequest;

@Getter
@Setter
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, defaultImpl = RestRequestWrapper.class)
@Deprecated(since = "v1.1")
public class RestRequestWrapper implements EventTrace, Wrapper<RestRequest> {

    @Delegate
    @JsonIgnore
    private final RestRequest request = new RestRequest();

    // Move ExceptionInfo to RestSession
    private ExceptionInfo exception;

    private RestSessionWrapper remoteTrace;

    @Override
    public RestRequest unwrap() {
        return request;
    }
}
