package org.usf.inspect.server.model;

import org.usf.inspect.core.MainSession;

import com.fasterxml.jackson.annotation.JsonTypeName;

import lombok.Getter;
import lombok.Setter;


@JsonTypeName("main")
@Getter
@Setter
public class ServerMainSession extends MainSession implements ServerSession {
    private String instanceId;
    private String appName;
    private int mask;
}
