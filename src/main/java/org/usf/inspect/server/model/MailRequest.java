package org.usf.inspect.server.model;


import static org.usf.inspect.server.Utils.isEmpty;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class MailRequest extends SessionStage {
    private String host;
    private Integer port;
    @Deprecated(since = "v1.1", forRemoval = true)
    private List<MailRequestStage> actions;
    private List<Mail> mails;


    @Deprecated(since = "v1.1", forRemoval = true)
    private boolean status;

    private boolean failed;
    public void setStatus(boolean status) {
        failed = status;
    }

    public void updateIdRequest() {
        if(!isEmpty(getActions())) {
            var inc = new AtomicInteger(0);
            for (MailRequestStage stage : getActions()) {
                stage.setIdRequest(getIdRequest());
                stage.setOrder(inc.incrementAndGet());
            }
        }
        if(!isEmpty(getMails())) {
            for(Mail mail: getMails()) {
                mail.setIdRequest(getIdRequest());
            }
        }
    }
}
