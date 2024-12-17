package org.usf.inspect.server.model.object;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MailRequestStage extends RequestStage {
    private long id; //Todo cdRequest
    private int order;
}
