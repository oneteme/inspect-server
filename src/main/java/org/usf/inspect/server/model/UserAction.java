package org.usf.inspect.server.model;

import java.time.Instant;
import java.util.UUID;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Deprecated(forRemoval = true)
@RequiredArgsConstructor
public class UserAction {
	
	private final Instant start;
	private final String type; //web events, log 
    private final String name; //TODO value
    private final String nodeName; 
    private String cdSession; //TODO UUID sessionId;

}
