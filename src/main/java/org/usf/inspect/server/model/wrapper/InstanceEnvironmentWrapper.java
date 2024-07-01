package org.usf.inspect.server.model.wrapper;

import lombok.Getter;
import lombok.Setter;
import org.usf.traceapi.core.InstanceEnvironment;
import org.usf.traceapi.core.InstanceType;

import java.time.Instant;

@Getter
@Setter
public class InstanceEnvironmentWrapper extends InstanceEnvironment {

    private String id;

    public InstanceEnvironmentWrapper(String id, String name, String version, String address, String env, String os, String re, String user, InstanceType type, Instant instant, String collector) {
        super(name, version, address, env, os, re, user, type, instant, collector);
        this.id = id;
    }

    public InstanceEnvironmentWrapper withAddress(String address) {
        return new InstanceEnvironmentWrapper(getId(), getName(), getVersion(), address, getEnv(), getOs(), getRe(), getUser(), getType(), getInstant(), getCollector());
    }
}
