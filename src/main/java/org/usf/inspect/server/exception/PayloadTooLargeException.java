package org.usf.inspect.server.exception;

import static org.springframework.http.HttpStatus.PAYLOAD_TOO_LARGE;

import org.springframework.web.bind.annotation.ResponseStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@SuppressWarnings("serial")
@RequiredArgsConstructor
@ResponseStatus(value = PAYLOAD_TOO_LARGE) //TODO move this annotation to the global exception handler
public final class PayloadTooLargeException extends RuntimeException {

}
