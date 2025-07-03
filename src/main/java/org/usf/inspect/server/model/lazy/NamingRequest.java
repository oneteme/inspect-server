package org.usf.inspect.server.model.lazy;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.usf.inspect.server.Utils.isEmpty;

@Getter
@Setter
public class NamingRequest extends SessionStage {
    private String protocol;
    private String host;
    private Integer port;
    @Deprecated(since = "v1.1", forRemoval = true)
    private List<NamingRequestStage> actions;

    private boolean status;

    public void updateIdRequest() {
        if(!isEmpty(getActions())) {
            var inc = new AtomicInteger(0);
            for(NamingRequestStage stage: getActions()) {
                stage.setIdRequest(getIdRequest());
                stage.setOrder(inc.incrementAndGet());
            }
        }
    }
}
