package org.usf.trace.api.server.model.wrapper;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Delegate;
import org.usf.traceapi.core.RestRequest;

import java.util.function.Supplier;

@Getter
@Setter
public class RestRequestWrapper {
    @Delegate
    private final RestRequest request;
    private final String parentId;
    private long idRestRequest;

    public RestRequestWrapper(String parentId, Supplier<? extends RestRequest> fn) {
        this.parentId = parentId;
        this.request = fn.get(); //delegated setters
    }

    public RestRequestWrapper(String parentId, RestRequest request) {
        this.parentId = parentId;
        this.request = request; //delegated getters
    }
}
