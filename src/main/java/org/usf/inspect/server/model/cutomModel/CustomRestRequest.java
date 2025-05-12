package org.usf.inspect.server.model.cutomModel;

import lombok.Getter;
import lombok.Setter;
import org.usf.inspect.server.model.RestRequest;

@Getter
@Setter
public class CustomRestRequest extends RestRequest {
    protected String appName;

}
