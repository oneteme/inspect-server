package org.usf.trace.api.server.model.filter;

import lombok.Getter;
import lombok.Setter;
import org.usf.jquery.core.DBFilter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.usf.jquery.core.Utils.isEmpty;
import static org.usf.trace.api.server.config.TraceApiColumn.*;
import static org.usf.trace.api.server.config.TraceApiTable.APISESSION;

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
        if(!isEmpty(getIds())) {
            filters.add(APISESSION.column(ID).in(getIds()));
        }
        if(!isEmpty(getApiNames())) {
            filters.add(APISESSION.column(API_NAME).in(getApiNames()));
        }
        if(!isEmpty(getAppNames())) {
            filters.add(APISESSION.column(APP_NAME).in(getAppNames()));
        }
        if(!isEmpty(getMethods())) {
            filters.add(APISESSION.column(METHOD).in(getMethods()));
        }
        if(!isEmpty(getProtocols())) {
            filters.add(APISESSION.column(PROTOCOL).in(getProtocols()));
        }
        if(!isEmpty(getHosts())) {
            filters.add(APISESSION.column(HOST).in(getHosts()));
        }
        if(!isEmpty(getPorts())) {
            filters.add(APISESSION.column(PORT).in(getPorts()));
        }
        if(getPath() != null) {
            filters.add(APISESSION.column(PATH).like(getPath()));
        }
        if(getQuery() != null) {
            filters.add(APISESSION.column(QUERY).like(getQuery()));
        }
        if(!isEmpty(getMedias())) {
            filters.add(APISESSION.column(MEDIA).in(getMedias()));
        }
        if(!isEmpty(getAuths())) {
            filters.add(APISESSION.column(AUTH).in(getAuths()));
        }
        if(!isEmpty(getStatus())) {
            filters.add(APISESSION.column(STATUS).in(getStatus()));
        }
        if(!isEmpty(getUsers())) {
            filters.add(APISESSION.column(USER).in(getUsers()));
        }
        if(!isEmpty(getEnvironments())) {
            filters.add(APISESSION.column(ENVIRONEMENT).in(getEnvironments()));
        }
        if(getStart() != null) {
            filters.add(APISESSION.column(START).greaterOrEqual(getStart()));
        }
        if(getEnd() != null) {
            filters.add(APISESSION.column(END).lessThan(getEnd()));
        }
        return filters.toArray(DBFilter[]::new);
    }
}
