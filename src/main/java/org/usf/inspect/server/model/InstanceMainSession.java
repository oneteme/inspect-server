package org.usf.inspect.server.model;

import org.usf.inspect.core.InstanceEnvironment;
import org.usf.inspect.core.MainSession;

import com.fasterxml.jackson.annotation.JsonTypeName;

import lombok.Getter;
import lombok.Setter;


@JsonTypeName("main")
@Getter
@Setter
public class InstanceMainSession extends MainSession implements InstanceSession {
    private String instanceId;
    private String instanceUser;
    private String appName;
    private int mask;
}
