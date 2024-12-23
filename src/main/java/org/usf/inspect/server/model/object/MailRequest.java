package org.usf.inspect.server.model.object;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class MailRequest extends SessionStage {
    private String host;
    private int port;
    private List<MailRequestStage> actions;
    private List<Mail> mails;

    private long id;
    private String cdSession;
    private boolean status;
}
