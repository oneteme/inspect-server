package org.usf.trace.api.server.model.wrapper;

import lombok.Getter;
import lombok.Setter;
import org.usf.traceapi.core.InstanceEnvironment;
import org.usf.traceapi.core.InstanceType;

import java.time.Instant;

@Getter
@Setter
public class InstanceEnvironmentWrapper extends InstanceEnvironment {

    private String instanceId; //TODO rename id

    public InstanceEnvironmentWrapper(String instanceId, String name, String version, String address, String env, String os, String re, String user, InstanceType type, Instant instant, String collector) {
        super(name, version, address, env, os, re, user, type, instant, collector);
        this.instanceId = instanceId;
    }

    public InstanceEnvironmentWrapper withAddress(String address) {
        return new InstanceEnvironmentWrapper(getInstanceId(), getName(), getVersion(), address, getEnv(), getOs(), getRe(), getUser(), getType(), getInstant(), getCollector());
    }
}
