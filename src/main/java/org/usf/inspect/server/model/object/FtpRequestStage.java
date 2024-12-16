package org.usf.inspect.server.model.object;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FtpRequestStage extends RequestStage {
    private String[] args;

    private long idRequest;
    private int order;
}
