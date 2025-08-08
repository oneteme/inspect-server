package org.usf.inspect.server.model.wrapper;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Delegate;
import org.usf.inspect.core.Mail;
import org.usf.inspect.core.MailRequest;

@Setter
@Getter
public final class MailWrapper {
    @Delegate
    @JsonIgnore
    private final Mail mail = new Mail();
    private String requestId;
}
