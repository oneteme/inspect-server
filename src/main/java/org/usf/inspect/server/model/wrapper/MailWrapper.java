package org.usf.inspect.server.model.wrapper;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Delegate;
import org.usf.inspect.core.Mail;

@Getter
@Setter
@RequiredArgsConstructor
public class MailWrapper {
    private final long id;

    @Delegate
    private final Mail mail;
}
