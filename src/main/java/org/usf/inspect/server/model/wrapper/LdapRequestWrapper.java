package org.usf.inspect.server.model.wrapper;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Delegate;
import org.usf.inspect.core.NamingRequest;

@Getter
@Setter
@RequiredArgsConstructor
public class LdapRequestWrapper {

    private final String parentId;
    @Delegate
    private final NamingRequest ldapRequest;
    private long id;
}
