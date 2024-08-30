package org.usf.inspect.server.model.wrapper;

import java.util.function.Supplier;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.usf.inspect.core.RestRequest;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Delegate;

@Getter
@Setter
public class RestRequestWrapper {

    private long idRequest;
    private final String cdSession;
    @Delegate
    @JsonIgnore
    private final RestRequest request;

    public RestRequestWrapper(String cdSession, Supplier<? extends RestRequest> fn) {
        this.cdSession = cdSession;
        this.request = fn.get(); //delegated setters
    }

    public RestRequestWrapper(String cdSession, RestRequest request) {
        this.cdSession = cdSession;
        this.request = request; //delegated getters
    }
}
