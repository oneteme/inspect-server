package org.usf.inspect.server.model;

import org.usf.inspect.MailRequestSignal;
import org.usf.inspect.MailRequestUpdate;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author u$f
 *
 */
@Setter
@Getter
public class MailRequest extends AbstractRequest {

    private String protocol; //smtp(s), imap, pop3
    private String host;
    private int port;

    @JsonCreator public MailRequest() {
        // empty
    }
}
