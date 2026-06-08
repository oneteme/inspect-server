package org.usf.inspect.server.exception;

import static org.springframework.http.HttpStatus.PAYLOAD_TOO_LARGE;
import static org.springframework.http.ResponseEntity.status;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler //TODO choose between @ControllerAdvice and @RestControllerAdvice
    public ResponseEntity<Map<String, String>> handlePayloadTooLargeException(PayloadTooLargeException ex){
        Map<String, String> body = new HashMap<>();
        body.put("error","PAYLOAD_TOO_LARGE"); //TODO realy need this ? client can easily detect it from http status code
        body.put("message","Données trop volumineuses, Veuillez affiner votre requête"); //TODO i18n
        return status(PAYLOAD_TOO_LARGE).body(body); //TODO use Map.of
    }
}
