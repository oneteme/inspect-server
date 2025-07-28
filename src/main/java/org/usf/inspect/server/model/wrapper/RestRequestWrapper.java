package org.usf.inspect.server.model.wrapper;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Delegate;
import org.usf.inspect.core.ExceptionInfo;
import org.usf.inspect.core.RestRequest;
import org.usf.inspect.server.model.InstanceEventTrace;
import org.usf.inspect.server.model.Wrapper;

@Getter
@Setter
public class RestRequestWrapper extends InstanceEventTrace implements Wrapper<RestRequest> {

    @Delegate
    @JsonIgnore
    private final RestRequest request = new RestRequest();

    // Move ExceptionInfo to RestSession
    @Deprecated(since = "v1.1")
    private ExceptionInfo exception;

    private RestSessionWrapper remoteTrace;

    @Override
    public RestRequest unwrap() {
        return request;
    }
}
