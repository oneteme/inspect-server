package org.usf.trace.api.server.model.wrapper;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Delegate;
import org.usf.traceapi.core.FtpRequest;

@Getter
@Setter
@RequiredArgsConstructor
public class FtpRequestWrapper {

    private final String parentId;
    @Delegate
    private final FtpRequest ftpRequest;
    private long id;
}
