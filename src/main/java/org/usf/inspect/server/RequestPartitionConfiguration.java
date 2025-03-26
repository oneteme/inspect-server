package org.usf.inspect.server;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;


import java.time.temporal.ChronoUnit;
import static java.time.temporal.ChronoUnit.MONTHS;
@Setter
@Getter
@ToString
public class RequestPartitionConfiguration {
    private ChronoUnit http = MONTHS;
    private ChronoUnit jdbc = MONTHS;
    private ChronoUnit smtp = MONTHS;
    private ChronoUnit ldap = MONTHS;
    private ChronoUnit ftp = MONTHS;
    private ChronoUnit local = MONTHS;
}
