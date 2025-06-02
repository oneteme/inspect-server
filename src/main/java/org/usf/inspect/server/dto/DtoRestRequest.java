package org.usf.inspect.server.dto;

import lombok.Getter;
import lombok.Setter;
import org.usf.inspect.server.model.RestRequest;

@Getter
@Setter
public class DtoRestRequest extends RestRequest {
    protected String appName;
    protected String type;
    protected String sessionType;
    protected String parent;
}
