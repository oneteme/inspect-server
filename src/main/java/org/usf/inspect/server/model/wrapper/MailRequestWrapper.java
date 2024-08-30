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
public class MailRequestWrapper {

    private long id;
    private boolean completed;
    private final String cdSession;
    @JsonIgnore
    @Delegate
    private final MailRequest smtpRequest;
}
