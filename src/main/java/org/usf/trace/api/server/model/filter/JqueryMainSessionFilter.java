package org.usf.trace.api.server.model.filter;

import lombok.Getter;
import lombok.Setter;
import org.usf.jquery.core.DBFilter;
import org.usf.trace.api.server.config.TraceApiTable;

import java.time.Instant;
import java.util.Collection;

import static org.usf.jquery.core.Utils.isEmpty;
import static org.usf.trace.api.server.config.TraceApiColumn.*;

@Getter
@Setter
public class JqueryMainSessionFilter extends JquerySessionFilter {
    private final String[] names;
    private final String[] launchModes;
    private final String location;

    public JqueryMainSessionFilter(String[] ids, String[] appNames, String[] environments, String[] users, Instant start, Instant end, String[] names, String[] launchModes, String location) {
        super(ids, appNames, environments, users, start, end);
        this.names = names;
        this.launchModes = launchModes;
        this.location = location;
    }

    public JqueryMainSessionFilter(String[] ids) {
        this(ids, null, null, null, null, null, null, null, null);
    }
    @Override
    public Collection<DBFilter> filters(TraceApiTable table) {
        Collection<DBFilter> filters = super.filters(table);
        if(!isEmpty(getNames())) {
            filters.add(table.column(NAME).in(getNames()));
        }
        if(!isEmpty(getLaunchModes())) {
            filters.add(table.column(TYPE).in(getLaunchModes()));
        }
        if(getLocation() != null) {
            filters.add(table.column(METHOD).like(getLocation()));
        }
        return filters;
    }
}
