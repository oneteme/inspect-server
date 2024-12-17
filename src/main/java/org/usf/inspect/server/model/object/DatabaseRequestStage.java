package org.usf.inspect.server.model.object;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public final class DatabaseRequestStage extends RequestStage {
    private long[] count;

    private long id;
    private int order;
}
