package org.usf.trace.api.server.jquery.filter;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@AllArgsConstructor
public class JquerySessionFilter {
    private final String[] ids;
    private final String[] appNames;
    private final String[] environments;
    private final Instant start;
    private final Instant end;
}
