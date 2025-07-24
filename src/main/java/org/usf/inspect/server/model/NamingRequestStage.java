package org.usf.inspect.server.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NamingRequestStage extends AbstractStage {
    private String[] args;
}
