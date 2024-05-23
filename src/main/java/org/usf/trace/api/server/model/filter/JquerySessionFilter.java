package org.usf.trace.api.server.model.filter;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.usf.jquery.core.DBFilter;
import org.usf.trace.api.server.config.TraceApiTable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;

import static org.usf.jquery.core.Utils.isEmpty;
import static org.usf.trace.api.server.config.TraceApiColumn.*;

@Getter
@Setter
@AllArgsConstructor
public class JquerySessionFilter {
    private final String[] ids;
    private final String[] appNames;
    private final String[] environments;
    private final Instant start;
    private final Instant end;
    private final boolean lazy;

    public Collection<DBFilter> filters(TraceApiTable table) {
        Collection<DBFilter> filters = new ArrayList<>();
        if(!isEmpty(getIds())) {
            filters.add(table.column(ID).in(getIds()));
        }
        if(!isEmpty(getAppNames())) {
            filters.add(table.column(APP_NAME).in(getAppNames()));
        }
        if(!isEmpty(getEnvironments())) {
            filters.add(table.column(ENVIRONEMENT).in(getEnvironments()));
        }
        if(getStart() != null) {
            filters.add(table.column(START).greaterOrEqual(getStart()));
        }
        if(getEnd() != null) {
            filters.add(table.column(END).lessThan(getEnd()));
        }
        return filters;
    }
}
