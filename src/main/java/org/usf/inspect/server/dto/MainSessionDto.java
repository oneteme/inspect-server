package org.usf.inspect.server.dto;

import org.usf.inspect.server.model.MainSession;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MainSessionDto extends MainSession {
    private String appName;
    private String address;
}
