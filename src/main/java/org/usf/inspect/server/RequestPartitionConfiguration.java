package org.usf.inspect.server;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.usf.inspect.server.model.Partition;

@Setter
@Getter
@ToString
public class RequestPartitionConfiguration {

    private Partition http;
    private Partition jdbc;
    private Partition smtp;
    private Partition ldap;
    private Partition ftp;
    private Partition local;

}
