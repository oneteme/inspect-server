package org.usf.inspect.server.dto;

import lombok.Getter;
import lombok.Setter;

import org.usf.inspect.SessionEvent;
import org.usf.inspect.server.model.MainSession;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class SessionEventDto extends MainSession {
    private Instant instant;
    private String type;
    private String value;
    private UUID sessionId;
    private String namespace;
    private String appName;
    private List<SessionEvent> sessionEvents;
}
