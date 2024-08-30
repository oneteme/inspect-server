package org.usf.inspect.server.model;

import java.time.Instant;

import org.usf.inspect.core.InstanceEnvironment;
import org.usf.inspect.core.InstanceType;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ServerInstanceEnvironment extends InstanceEnvironment {

    private String id;

    public ServerInstanceEnvironment(String id, String name, String version, String address, String env, String os, String re, String user, InstanceType type, Instant instant, String collector) {
        super(name, version, address, env, os, re, user, type, instant, collector);
        this.id = id;
    }

    public ServerInstanceEnvironment withAddress(String address) {
        return new ServerInstanceEnvironment(getId(), getName(), getVersion(), address, getEnv(), getOs(), getRe(), getUser(), getType(), getInstant(), getCollector());
    }
}
