package org.usf.inspect.server.model.filter;

import static java.sql.Timestamp.from;
import static org.usf.inspect.server.config.TraceApiColumn.*;
import static org.usf.inspect.server.config.TraceApiTable.INSTANCE;
import static org.usf.jquery.core.Utils.isEmpty;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;

import org.usf.inspect.server.config.TraceApiTable;
import org.usf.jquery.core.DBFilter;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class JqueryRequestFilter {
    private final String[] environments;
    private final String[] hosts;
    private final Instant start;
    private final Instant end;
    private final Boolean[] failed;

    public Collection<DBFilter> filters(TraceApiTable table) {
        Collection<DBFilter> filters = new ArrayList<>();
        if(!isEmpty(getEnvironments())) {
            filters.add(INSTANCE.column(ENVIRONEMENT).in(getEnvironments()));
        }
        if(!isEmpty(getHosts())) {
            filters.add(table.column(HOST).in(getHosts()));
        }
        if(getStart() != null) {
            filters.add(table.column(START).ge(from(getStart())));
        }
        if(getEnd() != null) {
            filters.add(table.column(START).lt(from(getEnd()))); // to be fixed
        }
        if(getFailed() != null) {
            filters.add(table.column(FAILED).in(getFailed()));
        }
        return filters;
    }
}
