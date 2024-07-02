package org.usf.inspect.server.model.wrapper;

import org.usf.inspect.core.FtpRequest;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Delegate;

@Getter
@Setter
@RequiredArgsConstructor
public class FtpRequestWrapper {

    private final String parentId;
    @Delegate
    private final FtpRequest ftpRequest;
    private long id;
}
