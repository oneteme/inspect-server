package org.usf.inspect.server.dto;

import lombok.Getter;
import lombok.Setter;
import org.usf.inspect.core.RestSession;

@Getter
@Setter
public class RestSessionDto extends RestSession {
    private String appName;
}
