package org.usf.inspect.server.model.filter;

import static org.usf.inspect.server.config.TraceApiColumn.*;
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
    private final Boolean[] failed;
    private final boolean lazy;

    public JqueryMainSessionFilter(String[] appNames, String[] environments, String[] users, Instant start, Instant end, String[] names, String[] launchModes, String location, Boolean[] failed, boolean lazy) {
        super(appNames, environments, users, start, end );
        this.names = names;
        this.launchModes = launchModes;
        this.location = location;
        this.failed = failed;
        this.lazy = lazy;
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

        if(!isEmpty(getFailed())){
            DBFilter filter;
            if(getFailed().length == 1) {
                if(getFailed()[0] == false) {
                    filter = table.column(ERR_TYPE).coalesce("null").ne("null").and(table.column(ERR_MSG).coalesce("null").ne("null"));
                } else {
                    filter = table.column(ERR_TYPE).coalesce("null").eq("null").and(table.column(ERR_MSG).coalesce("null").eq("null"));
                }
                if(isLazy()) {
                    filter.or(table.column(END).isNull());
                }
                filters.add(filter);
            }
        } else if(isLazy()) {
            filters.add(table.column(END).isNull());
        }
        return filters;
    }
}
