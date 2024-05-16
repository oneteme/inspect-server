package org.usf.trace.api.server.model.filter;

import lombok.Getter;
import lombok.Setter;
import org.usf.jquery.core.DBFilter;
import org.usf.jquery.core.Utils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.usf.trace.api.server.config.TraceApiColumn.*;
import static org.usf.trace.api.server.config.TraceApiColumn.END;
import static org.usf.trace.api.server.config.TraceApiTable.REQUEST;

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

    public DBFilter[] filters() {
        List<DBFilter> filters = new ArrayList<>();
        if(!Utils.isEmpty(getIds())) {
            filters.add(REQUEST.column(ID).in(getIds()));
        }
        if(!Utils.isEmpty(getApiNames())) {
            filters.add(REQUEST.column(API_NAME).in(getApiNames()));
        }
        if(!Utils.isEmpty(getAppNames())) {
            filters.add(REQUEST.column(APP_NAME).in(getAppNames()));
        }
        if(!Utils.isEmpty(getMethods())) {
            filters.add(REQUEST.column(METHOD).in(getMethods()));
        }
        if(!Utils.isEmpty(getProtocols())) {
            filters.add(REQUEST.column(PROTOCOL).in(getProtocols()));
        }
        if(!Utils.isEmpty(getHosts())) {
            filters.add(REQUEST.column(HOST).in(getHosts()));
        }
        if(!Utils.isEmpty(getPorts())) {
            filters.add(REQUEST.column(PORT).in(getPorts()));
        }
        if(getPath() != null) {
            filters.add(REQUEST.column(PATH).like(getPath()));
        }
        if(getQuery() != null) {
            filters.add(REQUEST.column(QUERY).like(getQuery()));
        }
        if(!Utils.isEmpty(getMedias())) {
            filters.add(REQUEST.column(MEDIA).in(getMedias()));
        }
        if(!Utils.isEmpty(getAuths())) {
            filters.add(REQUEST.column(AUTH).in(getAuths()));
        }
        if(!Utils.isEmpty(getStatus())) {
            filters.add(REQUEST.column(STATUS).in(getStatus()));
        }
        if(!Utils.isEmpty(getUsers())) {
            filters.add(REQUEST.column(USER).in(getUsers()));
        }
        if(!Utils.isEmpty(getEnvironments())) {
            filters.add(REQUEST.column(ENVIRONEMENT).in(getEnvironments()));
        }
        if(getStart() != null) {
            filters.add(REQUEST.column(START).greaterOrEqual(getStart()));
        }
        if(getEnd() != null) {
            filters.add(REQUEST.column(END).lessThan(getEnd()));
        }
        return filters.toArray(DBFilter[]::new);
    }
}
