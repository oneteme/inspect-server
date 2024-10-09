package org.usf.inspect.server.model.wrapper;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.usf.inspect.core.FtpRequest;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Delegate;

@Getter
@Setter
@RequiredArgsConstructor
public class FtpRequestWrapper {

    private long id;
    private boolean completed;

    private final String cdSession;
    @JsonIgnore
    @Delegate
    private final FtpRequest ftpRequest;

}
