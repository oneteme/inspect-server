package org.usf.inspect.server.model.wrapper;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Delegate;
import org.usf.inspect.core.NamingRequest;

@Getter
@Setter
@RequiredArgsConstructor
public class NamingRequestWrapper extends  NamingRequest {

    private long id;
    private boolean status;

    private final String cdSession;
    @JsonIgnore
    @Delegate
    private final NamingRequest ldapRequest;
}
