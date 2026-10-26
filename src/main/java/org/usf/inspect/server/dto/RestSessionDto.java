package org.usf.inspect.server.dto;

import org.usf.inspect.server.model.RestSession;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RestSessionDto extends RestSession {
    private String appName;
}
