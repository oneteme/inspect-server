package org.usf.inspect.server.exception;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.PAYLOAD_TOO_LARGE;
import static org.springframework.http.ResponseEntity.status;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.usf.jquery.core.LimitExceededException;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(LimitExceededException.class)
    public ResponseEntity<Map<String, String>> handlePayloadTooLargeException(LimitExceededException ex) {
        Map<String, String> body = new HashMap<>();
        body.put("error", "PAYLOAD_TOO_LARGE");
        body.put("message", "Données trop volumineuses, Veuillez affiner votre requête");
        return status(PAYLOAD_TOO_LARGE).body(body);
    }

    @ExceptionHandler(InvalidNamespaceException.class)
    public ResponseEntity<Map<String, String>> handleInvalidNamespaceException(InvalidNamespaceException ex) {
        Map<String, String> body = new HashMap<>();
        body.put("error", "NAMESPACE_INVALID");
        body.put("message", ex.getMessage());
        return status(BAD_REQUEST).body(body);
    }

    @ExceptionHandler(NamespaceAlreadyExistsException.class)
    public ResponseEntity<Map<String, String>> handleNamespaceAlreadyExistsException(NamespaceAlreadyExistsException ex) {
        Map<String, String> body = new HashMap<>();
        body.put("error", "NAMESPACE_ALREADY_EXIST");
        body.put("message", ex.getMessage());
        return status(CONFLICT).body(body);
    }
}
