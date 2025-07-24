package org.usf.inspect.server.model;

import static org.usf.inspect.server.Utils.isEmpty;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FtpRequest extends AbstractRequest {
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
                stage.setRequestId(getIdRequest());
                stage.setOrder(inc.incrementAndGet());
            }
        }
    }


}
