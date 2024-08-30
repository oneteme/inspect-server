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
public class LdapRequestWrapper {

    private long id;

    private final String parentId;
    @JsonIgnore
    @Delegate
    private final NamingRequest ldapRequest;
}
