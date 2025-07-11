package org.usf.inspect.server;

import org.usf.inspect.server.model.Partition;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

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
