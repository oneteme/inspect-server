package org.usf.inspect.server.model;

import java.time.Instant;
import java.util.Map;
import org.usf.inspect.core.InspectCollectorConfiguration;
import org.usf.inspect.core.InstanceType;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InstanceEnvironment {
    private final String name;
    private final String version;
    private String address;
    private final String env;
    private final String os;
    private final String re;
    private final String user;
    private final InstanceType type;
    private final Instant instant;
    private final String collector;
    private final String branch;
    private final String hash;
    private Instant end;
    private String id;

    //v1.1
    private final Map<String, String> additionalProperties; //json / additional properties, e.g. for docker container, kubernetes pod, etc.
    private final InspectCollectorConfiguration configuration; // json
    private MachineResourceUsage resource; //init/max heap +  init/max metaspace

    public InstanceEnvironment(String name, String version, String address, String env, String os, String re, String user, InstanceType type, Instant instant, String collector, String branch, String hash, Instant end, Map<String, String> additionalProperties, InspectCollectorConfiguration configuration, MachineResourceUsage resource) {
        this.name = name;
        this.version = version;
        this.address = address;
        this.env = env;
        this.os = os;
        this.re = re;
        this.user = user;
        this.type = type;
        this.instant = instant;
        this.collector = collector;
        this.branch = branch;
        this.hash = hash;
        this.end = end;
        this.additionalProperties = additionalProperties;
        this.configuration = configuration;
        this.resource = resource;
    }
}


