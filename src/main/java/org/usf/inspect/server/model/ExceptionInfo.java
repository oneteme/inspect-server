package org.usf.inspect.server.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
public class ExceptionInfo {
    private final String type;
    private final String message;

    private Long idRequest;
    private Integer order;
}
