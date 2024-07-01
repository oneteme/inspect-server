package org.usf.inspect.server.model;

import org.usf.inspect.core.Session;


public interface InstanceSession extends Session {
    String getInstanceId(); //UUID

    void setInstanceId(String id);
}
