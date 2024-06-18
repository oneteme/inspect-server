package org.usf.trace.api.server.model;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.usf.traceapi.core.Session;


public interface InstanceSession extends Session {
    String getInstanceId(); //UUID

    void setInstanceId(String id);
}
