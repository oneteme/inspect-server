package org.usf.inspect.server.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.usf.inspect.server.Utils.isEmpty;

@Getter
@Setter
public class FtpRequest extends SessionStage {
    private String protocol;
    private String host;
    private Integer port;
    private String serverVersion;
    private String clientVersion;
    @Deprecated(since = "v1.1", forRemoval = true)
    private List<FtpRequestStage> actions;


    @Deprecated(since = "v1.1", forRemoval = true)
    private boolean status;

    private boolean failed;
    public void setStatus(boolean status) {
        failed = status;
    }


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
