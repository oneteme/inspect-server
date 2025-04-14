package org.usf.inspect.server.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@RequiredArgsConstructor
public class UserAction {
    private final String name;
    private final String nodeName;
    private final String type;
    private final Instant start;
    private String cdSession;
}
