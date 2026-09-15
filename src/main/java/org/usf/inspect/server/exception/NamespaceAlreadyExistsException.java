package org.usf.inspect.server.exception;

public class NamespaceAlreadyExistsException extends RuntimeException {
    public NamespaceAlreadyExistsException(String message) {
        super(message);
    }
}
