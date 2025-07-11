package org.usf.inspect.server.model;

import static org.usf.inspect.server.Utils.isEmpty;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FtpRequest extends SessionStage {
    private String protocol;
    private String host;
    private Integer port;
    private String serverVersion;
    private String clientVersion;
    private List<FtpRequestStage> actions;

    private boolean status;

    public void updateIdRequest() {
        if (!isEmpty(getActions())) {
            var inc = new AtomicInteger(0);
            for(FtpRequestStage stage: getActions()) {
                stage.setIdRequest(getIdRequest());
                stage.setOrder(inc.incrementAndGet());
            }
        }
    }
}
