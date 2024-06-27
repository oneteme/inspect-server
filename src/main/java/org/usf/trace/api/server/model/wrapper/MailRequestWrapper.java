package org.usf.trace.api.server.model.wrapper;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Delegate;
import org.usf.traceapi.core.MailRequest;

@Getter
@Setter
@RequiredArgsConstructor
public class MailRequestWrapper {

    private final String parentId;
    @Delegate
    private final MailRequest mailRequest;
    private long id;
}
