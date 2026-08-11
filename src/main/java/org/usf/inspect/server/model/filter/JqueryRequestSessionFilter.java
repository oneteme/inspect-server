package org.usf.inspect.server.model.filter;

import static org.usf.inspect.server.config.TraceApiColumn.*;
import static org.usf.jquery.core.Utils.isEmpty;

import java.time.Instant;
import java.util.Collection;

import org.usf.inspect.server.config.TraceApiTable;
import org.usf.jquery.core.DBFilter;
import org.usf.jquery.core.LogicalOperator;

import lombok.Getter;
import lombok.Setter;

/**
 * Builds database filters for request sessions with request-specific criteria.
 */
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
    private final boolean lazy;

    /**
     * Creates a request session filter with session and request criteria.
     *
     * @param appNames the application names to include
     * @param environments the environments to include
     * @param users the users to include
     * @param start the inclusive lower bound for the start time
     * @param end the exclusive upper bound for the start time
     * @param methods the request methods to include
     * @param protocols the protocols to include
     * @param hosts the hosts to include
     * @param ports the ports to include
     * @param medias the media types to include
     * @param auths the authentication types to include
     * @param status the HTTP statuses to include
     * @param apiNames the API names to include
     * @param path the path pattern to match
     * @param query the query pattern to match
     * @param rangestatus the status code ranges to include
     * @param lazy whether unfinished requests should also be included
     */
    public JqueryRequestSessionFilter(String[] appNames, String[] environments, String[] users, Instant start, Instant end, String[] methods, String[] protocols, String[] hosts, String[] ports, String[] medias, String[] auths, Integer[] status, String[] apiNames, String path, String query,String[] rangestatus, boolean lazy) {
        super(appNames, environments, users, start, end);
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
        this.lazy = lazy;
    }

    /**
     * Creates the database filters that apply to the given request session table.
     *
     * @param table the trace table for which filters are created
     * @return the collection of filters matching this request session filter configuration
     */
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
            if(isLazy()) {
                filter.or(table.column(END).isNull());
            }
            filters.add(filter);

        } else if(isLazy()) {
            filters.add(table.column(END).isNull());
        }
        return filters;
    }

}
