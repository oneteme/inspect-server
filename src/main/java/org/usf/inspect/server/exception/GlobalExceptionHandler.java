package org.usf.inspect.server.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler
    public ResponseEntity<Map<String, String>> handlePayloadTooLargeException(PayloadTooLargeException ex){
        Map<String, String> body = new HashMap<>();
        body.put("error","PAYLOAD_TOO_LARGE");
        body.put("message","Données trop volumineuses, Veuillez affiner votre requête");
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(body);
    }
}
