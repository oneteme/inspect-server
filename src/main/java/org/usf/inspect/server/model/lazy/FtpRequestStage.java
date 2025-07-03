package org.usf.inspect.server.model.lazy;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FtpRequestStage extends RequestStage {
    private String[] args;
}
