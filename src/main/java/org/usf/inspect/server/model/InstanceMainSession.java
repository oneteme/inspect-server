package org.usf.inspect.server.model;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Getter;
import lombok.Setter;

import org.usf.traceapi.core.InstanceEnvironment;
import org.usf.traceapi.core.MainSession;


@JsonTypeName("main")
@Getter
@Setter
public class InstanceMainSession extends MainSession implements InstanceSession {
    private String instanceId;
    private String instanceUser;
    private String appName;
    private int mask;

    @Deprecated
	public InstanceEnvironment getApplication() {
		throw new UnsupportedOperationException();
	}
}
