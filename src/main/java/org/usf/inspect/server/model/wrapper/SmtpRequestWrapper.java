package org.usf.inspect.server.model.wrapper;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.usf.inspect.core.MailRequest;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Delegate;

@Getter
@Setter
@RequiredArgsConstructor
public class SmtpRequestWrapper {

    private long id;
    private final String cdSession;
    @JsonIgnore
    @Delegate
    private final MailRequest smtpRequest;
}
