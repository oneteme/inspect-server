package org.usf.inspect.server;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.temporal.ChronoUnit;
import static java.time.temporal.ChronoUnit.MONTHS;

@Setter
@Getter
@ToString
public class SessionPartitionConfiguration {
    private ChronoUnit http = MONTHS;
    private ChronoUnit main = MONTHS;
}
