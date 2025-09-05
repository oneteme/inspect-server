package org.usf.inspect.server.model.wrapper;

@Deprecated(since = "v1.1")
public interface Wrapper<T> {
    T unwrap();
}
