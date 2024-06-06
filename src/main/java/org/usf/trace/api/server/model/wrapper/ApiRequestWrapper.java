package org.usf.trace.api.server.model.wrapper;

import lombok.Getter;
import lombok.experimental.Delegate;
import org.usf.traceapi.core.RestRequest;

import java.util.function.Supplier;

@Getter
public class ApiRequestWrapper {
    @Delegate
    private final RestRequest request;
    private final String parentId;

    public ApiRequestWrapper(String parentId, Supplier<? extends RestRequest> fn) {
        this.parentId = parentId;
        this.request = fn.get(); //delegated setters
    }

    public ApiRequestWrapper(String parentId, RestRequest request) {
        this.parentId = parentId;
        this.request = request; //delegated getters
    }
}
