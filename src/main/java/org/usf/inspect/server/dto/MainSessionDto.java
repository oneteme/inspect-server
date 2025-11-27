package org.usf.inspect.server.dto;

import lombok.Getter;
import lombok.Setter;
import org.usf.inspect.server.model.MainSession;

@Getter
@Setter
public class MainSessionDto extends MainSession {
    private String appName;
}
