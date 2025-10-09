package org.usf.inspect.server.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@SuppressWarnings("serial")
@ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE) //GlobalExceptionHandler !!??
public class PayloadTooLargeException extends RuntimeException{

}
