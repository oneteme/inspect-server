package org.usf.inspect.server.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FtpRequestStage extends RequestStage {
    private String[] args;
}
