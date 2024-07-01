package org.usf.inspect.server.model.wrapper;

import java.util.function.Supplier;

import org.usf.inspect.core.RestRequest;

import lombok.Getter;
import lombok.experimental.Delegate;

@Getter
public class RestRequestWrapper {
    @Delegate
    private final RestRequest request;
    private final String parentId;

    public RestRequestWrapper(String parentId, Supplier<? extends RestRequest> fn) {
        this.parentId = parentId;
        this.request = fn.get(); //delegated setters
    }

    public RestRequestWrapper(String parentId, RestRequest request) {
        this.parentId = parentId;
        this.request = request; //delegated getters
    }
}
