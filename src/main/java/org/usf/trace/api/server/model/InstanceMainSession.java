package org.usf.trace.api.server.model;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Getter;
import lombok.Setter;
import org.usf.traceapi.core.MainSession;


@JsonTypeName("main")
@Getter
@Setter
public class InstanceMainSession extends MainSession implements InstanceSession {
    private String instanceId;
    private String appName;
}
