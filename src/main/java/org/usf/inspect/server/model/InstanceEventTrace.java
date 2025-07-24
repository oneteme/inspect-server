package org.usf.inspect.server.model;

import org.usf.inspect.core.EventTrace;

public interface InstanceEventTrace extends EventTrace {
    void setInstanceId(String instanceId);
}
