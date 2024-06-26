package org.usf.trace.api.server.model;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Getter;
import lombok.Setter;
import org.usf.traceapi.core.InstanceEnvironment;
import org.usf.traceapi.core.RestSession;

@JsonTypeName("api")
@Getter
@Setter
public class InstanceRestSession extends RestSession implements InstanceSession {
    private String instanceId;
    private String appName;
    
    @Deprecated
	public InstanceEnvironment getApplication() {
		throw new UnsupportedOperationException();
	}
}
