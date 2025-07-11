package org.usf.inspect.server.model;

import static org.usf.inspect.server.Utils.isEmpty;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NamingRequest extends SessionStage {
    private String protocol;
    private String host;
    private Integer port;
    @Deprecated(since = "v1.1", forRemoval = true)
    private List<NamingRequestStage> actions;

    @Deprecated(since = "v1.1", forRemoval = true)
    private boolean status;

    private boolean failed;
    public void setStatus(boolean status) {
        failed = status;
    }
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
