package org.usf.inspect.server.model;

import java.util.List;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class Architecture {
    private final String name;
    private final String schema;
    private final String type; // APP, FTP, SMTP, JDBC
    private final List<Architecture> remoteServers;
}
