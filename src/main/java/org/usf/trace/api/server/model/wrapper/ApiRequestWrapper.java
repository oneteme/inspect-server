package org.usf.trace.api.server.model.wrapper;

import lombok.Getter;
import lombok.experimental.Delegate;
import org.usf.traceapi.core.ApiRequest;

import java.util.function.Supplier;

@Getter
public class ApiRequestWrapper {
    @Delegate
    private final ApiRequest request;
    private final String parentId;

    public ApiRequestWrapper(String parentId, Supplier<? extends ApiRequest> fn) {
        this.parentId = parentId;
        this.request = fn.get(); //delegated setters
    }

    public ApiRequestWrapper(String parentId, ApiRequest request) {
        this.parentId = parentId;
        this.request = request; //delegated getters
    }
}
