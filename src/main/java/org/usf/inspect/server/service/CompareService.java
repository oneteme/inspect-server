package org.usf.inspect.server.service;

import static java.util.UUID.fromString;
import static org.usf.inspect.server.Utils.fromNullableTimestamp;
import static org.usf.inspect.server.Utils.requireSingle;
import static org.usf.inspect.server.config.TraceApiColumn.*;
import static org.usf.inspect.server.config.TraceApiDatabase.INSPECT;
import static org.usf.inspect.server.config.TraceApiTable.*;
import static org.usf.inspect.server.config.constant.JoinConstant.*;
import static org.usf.inspect.server.mapper.InspectMappers.getExceptionInfoIfNotNull;
import static org.usf.jquery.core.DBColumn.column;

import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.usf.inspect.server.model.wrapper.RestRequestWrapper;
import org.usf.inspect.server.model.wrapper.RestSessionWrapper;
import org.usf.jquery.core.NamedColumn;
import org.usf.jquery.core.QueryComposer;
import org.usf.jquery.web.ColumnDecorator;
import org.usf.jquery.web.ViewDecorator;

@Service
public class CompareService {

    public RestRequestWrapper getComparedSession(String id) {
        var session = getRestSessionById(id);
        if (session != null) {
            var request = getRestRequestById(id);
            if (request != null) {
                request.setRemoteTrace(session);
            }
            return request;
        }
        throw new NoSuchElementException("no rest session found for id: " + id);
    }

    private RestSessionWrapper getRestSessionById(String id) {
        var v = new QueryComposer()
                .columns(getColumns(
                        REST_SESSION, ID, API_NAME, METHOD,
                        PROTOCOL, HOST, PORT, PATH, QUERY, MEDIA, AUTH, STATUS, SIZE_IN, SIZE_OUT,
                        CONTENT_ENCODING_IN, CONTENT_ENCODING_OUT, START, END, THREAD,
                        ERR_TYPE, ERR_MSG, MASK, USER, USER_AGT, CACHE_CONTROL, INSTANCE_ENV
                ))
                .columns(getColumns(INSTANCE, APP_NAME, OS, RE, ADDRESS, BRANCH, HASH, ENVIRONEMENT))
                .filters(REST_SESSION.column(INSTANCE_ENV).eq(INSTANCE.column(ID))
                        .and(REST_SESSION.column(START).ge(INSTANCE.column(START))))
                .filters(column("id_ses").eq(fromString(id)));
        return requireSingle(INSPECT.execute(v, rs -> {
            var sessions = new ArrayList<RestSessionWrapper>();
            while (rs.next()) {
                var session = new RestSessionWrapper();
                session.setId(rs.getString(ID.reference()));
                session.setMethod(rs.getString(METHOD.reference()));
                session.setProtocol(rs.getString(PROTOCOL.reference()));
                session.setHost(rs.getString(HOST.reference()));
                session.setPort(rs.getInt(PORT.reference()));
                session.setPath(rs.getString(PATH.reference()));
                session.setQuery(rs.getString(QUERY.reference()));
                session.setContentType(rs.getString(MEDIA.reference()));
                session.setAuthScheme(rs.getString(AUTH.reference()));
                session.setStatus(rs.getInt(STATUS.reference()));
                session.setInDataSize(rs.getLong(SIZE_IN.reference()));
                session.setOutDataSize(rs.getLong(SIZE_OUT.reference()));
                session.setInContentEncoding(rs.getString(CONTENT_ENCODING_IN.reference()));
                session.setOutContentEncoding(rs.getString(CONTENT_ENCODING_OUT.reference()));
                session.setStart(fromNullableTimestamp(rs.getTimestamp(START.reference())));
                session.setEnd(fromNullableTimestamp(rs.getTimestamp(END.reference())));
                session.setThreadName(rs.getString(THREAD.reference()));
                session.setException(getExceptionInfoIfNotNull(rs.getString(ERR_TYPE.reference()), rs.getString(ERR_MSG.reference()), null));
                session.setName(rs.getString(API_NAME.reference()));
                session.setUserAgent(rs.getString(USER_AGT.reference()));
                session.setUser(rs.getString(USER.reference()));
                session.setInstanceId(rs.getString(INSTANCE_ENV.reference()));
                session.setCacheControl(rs.getString(CACHE_CONTROL.reference()));
                session.setOs(rs.getString(OS.reference()));
                session.setRe(rs.getString(RE.reference()));
                session.setAddress(rs.getString(ADDRESS.reference()));
                session.setAppName(rs.getString(APP_NAME.reference()));
                session.setRequestsMask(rs.getInt(MASK.reference()));
                session.setBranch(rs.getString(BRANCH.reference()));
                session.setHash(rs.getString(HASH.reference()));
                session.setEnvironment(rs.getString(ENVIRONEMENT.reference()));
                sessions.add(session);
            }
            return sessions;
        }));
    }

    private RestRequestWrapper getRestRequestById(String id) {
        var v = new QueryComposer()
                .columns(getColumns(
                        REST_REQUEST, ID, PROTOCOL, AUTH, HOST, PORT, PATH, QUERY, METHOD, STATUS,
                        SIZE_IN, SIZE_OUT, CONTENT_ENCODING_IN, CONTENT_ENCODING_OUT, START, END, THREAD, BODY_CONTENT, LINKED, PARENT
                ))
                .columns(getColumns(EXCEPTION, ERR_TYPE, ERR_MSG))
                .columns(getColumns(INSTANCE, APP_NAME, OS, RE, ADDRESS, ENVIRONEMENT, BRANCH, HASH))
                .joins(REST_REQUEST.join(EXCEPTION_JOIN))
                .joins(REST_REQUEST.join(INSTANCE_JOIN))
                .filters(REST_REQUEST.column(START).ge(INSTANCE.column(START)))
                .filters(column("id_rst_rqt").eq(fromString(id)));
        return requireSingle(INSPECT.execute(v, rs -> {
            var requests = new ArrayList<RestRequestWrapper>();
            while (rs.next()) {
                var request = new RestRequestWrapper();
                request.setSessionId(rs.getString(PARENT.reference()));
                request.setId(rs.getString(ID.reference()));
                request.setProtocol(rs.getString(PROTOCOL.reference()));
                request.setHost(rs.getString(HOST.reference()));
                request.setPort(rs.getInt(PORT.reference()));
                request.setPath(rs.getString(PATH.reference()));
                request.setQuery(rs.getString(QUERY.reference()));
                request.setMethod(rs.getString(METHOD.reference()));
                request.setStatus(rs.getInt(STATUS.reference()));
                request.setInDataSize(rs.getLong(SIZE_IN.reference()));
                request.setOutDataSize(rs.getLong(SIZE_OUT.reference()));
                request.setInContentEncoding(rs.getString(CONTENT_ENCODING_IN.reference()));
                request.setOutContentEncoding(rs.getString(CONTENT_ENCODING_OUT.reference()));
                request.setStart(fromNullableTimestamp(rs.getTimestamp(START.reference())));
                request.setEnd(fromNullableTimestamp(rs.getTimestamp(END.reference())));
                request.setThreadName(rs.getString(THREAD.reference()));
                request.setLinked(rs.getBoolean(LINKED.reference()));
                request.setAuthScheme(rs.getString(AUTH.reference()));
                request.setOs(rs.getString(OS.reference()));
                request.setRe(rs.getString(RE.reference()));
                request.setAddress(rs.getString(ADDRESS.reference()));
                request.setAppName(rs.getString(APP_NAME.reference()));
                request.setBodyContent(rs.getString(BODY_CONTENT.reference()));
                request.setBranch(rs.getString(BRANCH.reference()));
                request.setHash(rs.getString(HASH.reference()));
                request.setEnvironment(rs.getString(ENVIRONEMENT.reference()));
                request.setException(getExceptionInfoIfNotNull(rs.getString(ERR_TYPE.reference()), rs.getString(ERR_MSG.reference()), null));
                requests.add(request);
            }
            return requests;
        }));
    }

    private static NamedColumn[] getColumns(ViewDecorator table, ColumnDecorator... columns) {
        return Stream.of(columns).map(table::column).toArray(NamedColumn[]::new);
    }
}
