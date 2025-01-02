package org.usf.inspect.server.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class MailRequest extends SessionStage {
    private String host;
    private Integer port;
    private List<MailRequestStage> actions;
    private List<Mail> mails;

    private boolean status;
}
