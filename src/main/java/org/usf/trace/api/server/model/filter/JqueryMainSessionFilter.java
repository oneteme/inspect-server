package org.usf.trace.api.server.model.filter;

import lombok.Getter;
import lombok.Setter;
import org.usf.jquery.core.DBFilter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.usf.jquery.core.Utils.isEmpty;
import static org.usf.trace.api.server.config.TraceApiColumn.*;
import static org.usf.trace.api.server.config.TraceApiTable.APIREQUEST;
import static org.usf.trace.api.server.config.TraceApiTable.MAINSESSION;

@Getter
@Setter
public class JqueryMainSessionFilter extends JquerySessionFilter {
    private final String[] names;
    private final String[] launchModes;
    private final String location;

    public JqueryMainSessionFilter(String[] ids, String[] appNames, String[] environments, Instant start, Instant end, boolean lazy, String[] names, String[] launchModes, String location) {
        super(ids, appNames, environments, start, end, lazy);
        this.names = names;
        this.launchModes = launchModes;
        this.location = location;
    }

    public JqueryMainSessionFilter(String[] ids, boolean lazy) {
        this(ids, null, null, null, null, lazy, null, null, null);
    }

    public DBFilter[] filters() {
        List<DBFilter> filters = new ArrayList<>();
        if(!isEmpty(getIds())) {
            filters.add(MAINSESSION.column(ID).in(getIds()));
        }
        if(!isEmpty(getNames())) {
            filters.add(MAINSESSION.column(NAME).in(getNames()));
        }
        if(!isEmpty(getLaunchModes())) {
            filters.add(MAINSESSION.column(TYPE).in(getLaunchModes()));
        }
        if(getLocation() != null) {
            filters.add(APIREQUEST.column(METHOD).like(getLocation()));
        }
        if(getStart() != null) {
            filters.add(APIREQUEST.column(START).greaterOrEqual(getStart()));
        }
        if(getEnd() != null) {
            filters.add(APIREQUEST.column(END).lessThan(getEnd()));
        }
        return filters.toArray(DBFilter[]::new);
    }
}
