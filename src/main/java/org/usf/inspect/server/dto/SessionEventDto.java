package org.usf.inspect.server.dto;

import lombok.Getter;
import lombok.Setter;
import org.usf.inspect.core.SessionEvent;
import org.usf.inspect.server.model.MainSession;

import java.util.List;

@Getter
@Setter
public class SessionEventDto extends MainSession {
    private List<SessionEvent> sessionEvents;
}
