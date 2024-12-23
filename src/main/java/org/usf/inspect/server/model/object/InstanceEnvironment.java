package org.usf.inspect.server.model.object;

import lombok.*;
import org.usf.inspect.core.InstanceType;

import java.time.Instant;

@Getter
@Setter
@RequiredArgsConstructor
public class InstanceEnvironment {
    private final String name;
    private final String version;
    private final String address;
    private final String env;
    private final String os;
    private final String re;
    private final String user;
    private final InstanceType type;
    private final Instant instant;
    private final String collector;

    private String id;

    public InstanceEnvironment withAddress(String address) {
        var instance = new InstanceEnvironment(getName(), getVersion(), address, getEnv(), getOs(), getRe(), getUser(), getType(), getInstant(), getCollector());
        instance.setId(getId());
        return instance;
    }
}


