package org.usf.inspect.server.model.object;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class FtpRequest extends SessionStage {
    private String protocol;
    private String host;
    private int port;
    private String serverVersion;
    private String clientVersion;
    private List<FtpRequestStage> actions;

    private long id;
    private String cdSession;
    private boolean status;
}
