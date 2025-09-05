package org.usf.inspect.server;

import lombok.Getter;
import lombok.Setter;
import org.usf.inspect.server.model.Partition;

@Getter
@Setter
public class PartitionProperties {
    private boolean enabled = false;
    private Partition httpSession;
    private Partition mainSession;
    private Partition httpRequest;
    private Partition jdbcRequest;
    private Partition smtpRequest;
    private Partition ldapRequest;
    private Partition ftpRequest;
    private Partition localRequest;
    private Partition resourceUsage;
    private Partition instanceTrace;
}
