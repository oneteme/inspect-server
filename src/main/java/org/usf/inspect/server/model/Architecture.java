package org.usf.inspect.server.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class Architecture {
    private final String name;
    private final String schema;
    private final String type; // APP, FTP, SMTP, JDBC
    private final List<Architecture> remoteServers;
}
