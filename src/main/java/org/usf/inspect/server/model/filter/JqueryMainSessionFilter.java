package org.usf.inspect.server.model.filter;

import static org.usf.inspect.server.config.TraceApiColumn.ERR_MSG;
import static org.usf.inspect.server.config.TraceApiColumn.ERR_TYPE;
import static org.usf.inspect.server.config.TraceApiColumn.LOCATION;
import static org.usf.inspect.server.config.TraceApiColumn.NAME;
import static org.usf.inspect.server.config.TraceApiColumn.TYPE;
import static org.usf.jquery.core.Utils.isEmpty;

import java.time.Instant;
import java.util.Collection;

import org.usf.inspect.server.config.TraceApiTable;
import org.usf.jquery.core.DBFilter;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class JqueryMainSessionFilter extends JquerySessionFilter {
    private final String[] names;
    private final String[] launchModes;
    private final String location;
    private final String[] rangestatus;

    public JqueryMainSessionFilter(String[] appNames, String[] environments, String[] users, Instant start, Instant end, String[] names, String[] launchModes, String location, String[] rangestatus) {
        super(appNames, environments, users, start, end );
        this.names = names;
        this.launchModes = launchModes;
        this.location = location;
        this.rangestatus = rangestatus;
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
            filters.add(table.column(LOCATION).contentLike(getLocation()));
        }

        if(!isEmpty(getRangestatus()) && getRangestatus().length < 2){
                DBFilter filter;
                if(Boolean.parseBoolean(getRangestatus()[0])){
                    filter = table.column(ERR_TYPE).coalesce("null").eq("null").and(table.column(ERR_MSG).coalesce("null").eq("null"));
                }else {
                    filter = table.column(ERR_TYPE).coalesce("null").ne("null").and(table.column(ERR_MSG).coalesce("null").ne("null"));
                }
                filters.add(filter);
        }
        return filters;
    }
}
