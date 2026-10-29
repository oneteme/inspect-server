package org.usf.inspect.server.exception;

@SuppressWarnings("serial")
public class NamespaceAlreadyExistsException extends RuntimeException {
    public NamespaceAlreadyExistsException(String message) {
        super(message);
    }
}
