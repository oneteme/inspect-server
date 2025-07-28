package org.usf.inspect.server.dto;

import org.usf.inspect.server.model.wrapper.RestRequestWrapper;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DtoRestRequest extends RestRequestWrapper {
    protected String appName;
    protected String type;
    protected String sessionType;
    protected String parent;
}
