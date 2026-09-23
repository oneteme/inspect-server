package org.usf.inspect.server.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.Setter;
import org.usf.inspect.core.MailRequestSignal;
import org.usf.inspect.core.MailRequestUpdate;

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
