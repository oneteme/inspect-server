package org.usf.inspect.server.model;

import lombok.*;
import org.usf.inspect.core.InstanceType;

import java.time.Instant;

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
    private String end;

    private String id;

    public InstanceEnvironment(String name, String version, String address, String env, String os, String re, String user, InstanceType type, Instant instant, String collector, String branch, String hash) {
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
    }
}


