package org.usf.inspect.server.model.filter;

import static java.sql.Timestamp.from;
import static org.usf.inspect.server.config.TraceApiColumn.APP_NAME;
import static org.usf.inspect.server.config.TraceApiColumn.END;
import static org.usf.inspect.server.config.TraceApiColumn.ENVIRONEMENT;
import static org.usf.inspect.server.config.TraceApiColumn.ID;
import static org.usf.inspect.server.config.TraceApiColumn.START;
import static org.usf.inspect.server.config.TraceApiColumn.USER;
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
public class JquerySessionFilter {
    private final String[] ids;
    private final String[] appNames;
    private final String[] environments;
    private final String[] users;
    private final Instant start;
    private final Instant end;

    public Collection<DBFilter> filters(TraceApiTable table) {
        Collection<DBFilter> filters = new ArrayList<>();
        if(!isEmpty(getIds())) {
            filters.add(table.column(ID).in(getIds()));
        }
        if(!isEmpty(getAppNames())) {
            filters.add(INSTANCE.column(APP_NAME).in(getAppNames()));
        }
        if(!isEmpty(getEnvironments())) {
            filters.add(INSTANCE.column(ENVIRONEMENT).in(getEnvironments()));
        }
        if(!isEmpty(getUsers())) {
            filters.add(table.column(USER).in(getUsers()));
        }
        if(getStart() != null) {
            filters.add(table.column(START).ge(from(getStart())));
        }
        if(getEnd() != null) {
            filters.add(table.column(START).lt(from(getEnd()))); // to be fixed
        }
        return filters;
    }
}
