package org.usf.inspect.server.dto;

import lombok.Getter;
import lombok.Setter;
import org.usf.inspect.core.MainSession;

@Getter
@Setter
public class MainSessionDto extends MainSession {
    private String appName;
}
