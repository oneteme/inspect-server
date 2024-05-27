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
public class JqueryRequestSessionFilter extends JquerySessionFilter {
    private final String[] methods;
    private final String[] protocols;
    private final String[] hosts;
    private final String[] ports;
    private final String[] medias;
    private final String[] auths;
    private final String[] status;
    private final String[] apiNames;
    private final String[] users;
    private final String path;
    private final String query;

    public JqueryRequestSessionFilter(String[] ids, String[] appNames, String[] environments, Instant start, Instant end, boolean lazy, String[] methods, String[] protocols, String[] hosts, String[] ports, String[] medias, String[] auths, String[] status, String[] apiNames, String[] users, String path, String query) {
        super(ids, appNames, environments, start, end, lazy);
        this.methods = methods;
        this.protocols = protocols;
        this.hosts = hosts;
        this.ports = ports;
        this.medias = medias;
        this.auths = auths;
        this.status = status;
        this.apiNames = apiNames;
        this.users = users;
        this.path = path;
        this.query = query;
    }

    public JqueryRequestSessionFilter(String[] ids, boolean lazy) {
        this(ids, null,null,null,null, lazy, null,null,null,null,null,null,null,null, null,null,null);
    }

    @Override
    public Collection<DBFilter> filters(TraceApiTable table) {
        Collection<DBFilter> filters = super.filters(table);
        if(!isEmpty(getAppNames())) {
            filters.add(table.column(APP_NAME).in(getAppNames()));
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
        if(!isEmpty(getUsers())) {
            filters.add(table.column(USER).in(getUsers()));
        }
        return filters;
    }
}
