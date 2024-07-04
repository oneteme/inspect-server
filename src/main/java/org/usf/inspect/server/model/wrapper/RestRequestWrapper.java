package org.usf.inspect.server.model.wrapper;

import java.util.function.Supplier;

import org.usf.inspect.core.RestRequest;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Delegate;

@Getter
@Setter
public class RestRequestWrapper {
    @Delegate
    private final RestRequest request;
    private final String parentId;
    private long idRequest;

    public RestRequestWrapper(String parentId, Supplier<? extends RestRequest> fn) {
        this.parentId = parentId;
        this.request = fn.get(); //delegated setters
    }

    public RestRequestWrapper(String parentId, RestRequest request) {
        this.parentId = parentId;
        this.request = request; //delegated getters
    }
}
