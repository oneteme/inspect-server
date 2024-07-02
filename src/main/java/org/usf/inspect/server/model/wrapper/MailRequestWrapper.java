package org.usf.inspect.server.model.wrapper;

import org.usf.inspect.core.MailRequest;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Delegate;

@Getter
@Setter
@RequiredArgsConstructor
public class MailRequestWrapper {

    private final String parentId;
    @Delegate
    private final MailRequest mailRequest;
    private long id;
}
