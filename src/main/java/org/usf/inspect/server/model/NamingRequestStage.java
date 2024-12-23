package org.usf.inspect.server.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NamingRequestStage extends RequestStage {
    private String[] args;

    private long id;
    private int order;
}
