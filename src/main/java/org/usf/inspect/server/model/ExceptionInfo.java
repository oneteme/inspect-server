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

    private String idRequest;
    private Integer order;
}
