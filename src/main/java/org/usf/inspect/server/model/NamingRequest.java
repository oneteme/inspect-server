package org.usf.inspect.server.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class NamingRequest extends SessionStage {
    private String protocol;
    private String host;
    private Integer port;
    private List<NamingRequestStage> actions;

    private boolean status;
}
