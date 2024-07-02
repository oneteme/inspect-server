package org.usf.inspect.server.model.wrapper;

import java.time.Instant;

import org.usf.inspect.core.InstanceEnvironment;
import org.usf.inspect.core.InstanceType;

import lombok.Getter;
import lombok.Setter;

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
