package org.usf.inspect.server.model.filter;

import static org.usf.inspect.server.config.TraceApiColumn.API_NAME;
import static org.usf.inspect.server.config.TraceApiColumn.AUTH;
import static org.usf.inspect.server.config.TraceApiColumn.HOST;
import static org.usf.inspect.server.config.TraceApiColumn.MEDIA;
import static org.usf.inspect.server.config.TraceApiColumn.METHOD;
import static org.usf.inspect.server.config.TraceApiColumn.PATH;
import static org.usf.inspect.server.config.TraceApiColumn.PORT;
import static org.usf.inspect.server.config.TraceApiColumn.PROTOCOL;
import static org.usf.inspect.server.config.TraceApiColumn.QUERY;
import static org.usf.inspect.server.config.TraceApiColumn.STATUS;
import static org.usf.jquery.core.Utils.isEmpty;

import java.time.Instant;
import java.util.Collection;

import org.usf.inspect.server.config.TraceApiTable;
import org.usf.jquery.core.DBFilter;
import org.usf.jquery.core.LogicalOperator;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class JqueryRequestSessionFilter extends JquerySessionFilter {
    private final String[] methods;
    private final String[] protocols;
    private final String[] hosts;
    private final String[] ports;
    private final String[] medias;
    private final String[] auths;
    private final Integer[] status;
    private final String[] apiNames;
    private final String path;
    private final String query;
    private final String[] rangeStatus;

    public JqueryRequestSessionFilter(String[] ids, String[] appNames, String[] environments, String[] users, Instant start, Instant end, String[] methods, String[] protocols, String[] hosts, String[] ports, String[] medias, String[] auths, Integer[] status, String[] apiNames, String path, String query,String[] rangestatus) {
        super(ids, appNames, environments, users, start, end);
        this.methods = methods;
        this.protocols = protocols;
        this.hosts = hosts;
        this.ports = ports;
        this.medias = medias;
        this.auths = auths;
        this.status = status;
        this.apiNames = apiNames;
        this.path = path;
        this.query = query;
        this.rangeStatus = rangestatus;
    }

    public JqueryRequestSessionFilter(String[] ids) {
        this(ids, null,null,null,null, null, null,null,null,null,null,null,null, null,null,null,null);
    }

    @Override
    public Collection<DBFilter> filters(TraceApiTable table) {
        Collection<DBFilter> filters = super.filters(table);
        if(!isEmpty(getApiNames())) {
            filters.add(table.column(API_NAME).in(getApiNames()));
        }
        if(!isEmpty(getMethods())) {
            filters.add(table.column(METHOD).in(getMethods()));
        }
        if(!isEmpty(getProtocols())) {
            filters.add(table.column(PROTOCOL).in(getProtocols()));
        }
        if(!isEmpty(getHosts())) {
            filters.add(table.column(HOST).in(getHosts()));
        }
        if(!isEmpty(getPorts())) {
            filters.add(table.column(PORT).in(getPorts()));
        }
        if(getPath() != null) {
            filters.add(table.column(PATH).like(getPath()));
        }
        if(getQuery() != null) {
            filters.add(table.column(QUERY).like(getQuery()));
        }
        if(!isEmpty(getMedias())) {
            filters.add(table.column(MEDIA).in(getMedias()));
        }
        if(!isEmpty(getAuths())) {
            filters.add(table.column(AUTH).in(getAuths()));
        }
        if(!isEmpty(getStatus())) {
            filters.add(table.column(STATUS).in(getStatus()));
        }

        if(!isEmpty(getRangeStatus())){
            DBFilter filter = table.column(STATUS).varchar().startsLike(getRangeStatus()[0].charAt(0));
            for(int i = 1; i < getRangeStatus().length; i++) {
                filter = filter.append(LogicalOperator.OR, table.column(STATUS).varchar().startsLike(getRangeStatus()[i].charAt(0)));
            }
            filters.add(filter);
        }
        return filters;
    }

}
