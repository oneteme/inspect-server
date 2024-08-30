package org.usf.inspect.server.model;

import org.usf.inspect.core.RestSession;

import com.fasterxml.jackson.annotation.JsonTypeName;

import lombok.Getter;
import lombok.Setter;

@JsonTypeName("rest")
@Getter
@Setter
public class ServerRestSession extends RestSession implements ServerSession {
    private String instanceId;
    private String appName;
    private int mask;
}
