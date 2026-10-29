package org.usf.inspect.server.model.wrapper;

import org.usf.inspect.EventTrace;
import org.usf.inspect.ExceptionTrace;
import org.usf.inspect.server.model.RestRequest;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Delegate;

@Getter
@Setter
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, defaultImpl = RestRequestWrapper.class)
@Deprecated(since = "v1.1")
public class RestRequestWrapper implements EventTrace {

    @Delegate
    @JsonIgnore
    private final RestRequest request = new RestRequest();

    // Move ExceptionTrace to RestSession
    private ExceptionTrace exception;

    private RestSessionWrapper remoteTrace;

    private String appName;
    private String os;
    private String re;
    private String address;
    private String branch;
    private String hash;
    private String environment;
    private String version;
    private String name;

}
