package org.usf.inspect.server.model.object;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class NamingRequest extends SessionStage {
    private String protocol;
    private String host;
    private int port;
    private List<NamingRequestStage> actions;

    private long id;
    private String cdSession;
    private boolean status;
}
