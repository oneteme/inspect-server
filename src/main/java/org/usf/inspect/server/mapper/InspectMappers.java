package org.usf.inspect.server.mapper;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.usf.inspect.core.InstanceType;
import org.usf.inspect.server.RequestMask;
import org.usf.inspect.server.dto.DtoRequest;
import org.usf.inspect.server.model.*;
import org.usf.jquery.core.RowMapper;

import java.util.ArrayList;

import static org.usf.inspect.server.Utils.fromNullableTimestamp;
import static org.usf.inspect.server.config.TraceApiColumn.*;
import static org.usf.inspect.server.config.TraceApiColumn.ID;
import static org.usf.inspect.server.service.RequestService.getExceptionInfoIfNotNull;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class InspectMappers {

    public static RowMapper<InstanceEnvironment> instanceEnvironmentMapper() {
        return rs -> {
            var instanceEnvironment = new InstanceEnvironment(
                    rs.getString(APP_NAME.reference()),
                    rs.getString(VERSION.reference()),
                    rs.getString(ADDRESS.reference()),
                    rs.getString(ENVIRONEMENT.reference()),
                    rs.getString(OS.reference()),
                    rs.getString(RE.reference()),
                    rs.getString(USER.reference()),
                    InstanceType.valueOf(rs.getString(TYPE.reference())),
                    fromNullableTimestamp(rs.getTimestamp(START.reference())),
                    rs.getString(COLLECTOR.reference()),
                    rs.getString(BRANCH.reference()),
                    rs.getString(HASH.reference()),
                    fromNullableTimestamp(rs.getTimestamp(END.reference())));
            instanceEnvironment.setId(rs.getString(ID.reference()));
            return instanceEnvironment;
        };
    }

    public static RowMapper<RestRequest> restRequestMapper() {
        return rs -> {
           var out = new RestRequest();
            out.setCdSession(rs.getString(PARENT.reference()));
            out.setIdRequest(rs.getLong(ID.reference()));
            out.setId(rs.getString(REMOTE.reference()));
            out.setProtocol(rs.getString(PROTOCOL.reference()));
            out.setHost(rs.getString(HOST.reference()));
            out.setPort(rs.getInt(PORT.reference()));
            out.setPath(rs.getString(PATH.reference()));
            out.setQuery(rs.getString(QUERY.reference()));
            out.setMethod(rs.getString(METHOD.reference()));
            out.setStatus(rs.getInt(STATUS.reference()));
            out.setInDataSize(rs.getLong(SIZE_IN.reference()));
            out.setOutDataSize(rs.getLong(SIZE_OUT.reference()));
            out.setInContentEncoding(rs.getString(CONTENT_ENCODING_IN.reference()));
            out.setOutContentEncoding(rs.getString(CONTENT_ENCODING_OUT.reference()));
            out.setStart(fromNullableTimestamp(rs.getTimestamp(START.reference())));
            out.setEnd(fromNullableTimestamp(rs.getTimestamp(END.reference())));
            out.setThreadName(rs.getString(THREAD.reference()));
            out.setAuthScheme(rs.getString(AUTH.reference()));
            out.setException(getExceptionInfoIfNotNull(rs.getString(ERR_TYPE.reference()), rs.getString(ERR_MSG.reference())));
            return out;
        };
    }
    
    public static RowMapper<DtoRequest> databaseRequestMapper() {
        return rs -> {
            DtoRequest out = new DtoRequest();
            out.setIdRequest(rs.getLong(ID.reference()));
            out.setHost(rs.getString(HOST.reference()));
            out.setName(rs.getString(DB.reference()));
            out.setStart(fromNullableTimestamp(rs.getTimestamp(START.reference())));
            out.setEnd(fromNullableTimestamp(rs.getTimestamp(END.reference())));
            out.setThreadName(rs.getString(THREAD.reference()));
            out.setCommand(rs.getString(COMMAND.reference()));
            out.setStatus(rs.getBoolean(STATUS.reference()));
            out.setSchema(rs.getString(SCHEMA.reference()));
            out.setId(rs.getString(PARENT.reference()));
            out.setType(rs.getString(TYPE.reference()));
            out.setSessionType(rs.getString("sessiontype"));
            out.setException(getExceptionInfoIfNotNull(rs.getString(ERR_TYPE.reference()), rs.getString(ERR_MSG.reference())));
            return out;
        };
    }
    
    public static RowMapper<Session> restSessionShallowMapper() {
        return rs -> {
            RestSession out = new RestSession();
            out.setId(rs.getString(ID.reference()));
            out.setMethod(rs.getString(METHOD.reference()));
            out.setProtocol(rs.getString(PROTOCOL.reference()));
            out.setPath(rs.getString(PATH.reference()));
            out.setQuery(rs.getString(QUERY.reference()));
            out.setStatus(rs.getInt(STATUS.reference()));
            out.setInDataSize(rs.getLong(SIZE_IN.reference()));
            out.setOutDataSize(rs.getLong(SIZE_OUT.reference()));
            out.setStart(fromNullableTimestamp(rs.getTimestamp(START.reference())));
            out.setEnd(fromNullableTimestamp(rs.getTimestamp(END.reference())));
            out.setName(rs.getString(API_NAME.reference()));
            out.setUser(rs.getString(USER.reference()));
            out.setAppName(rs.getString(APP_NAME.reference()));
            out.setException(getExceptionInfoIfNotNull(rs.getString(ERR_TYPE.reference()), rs.getString(ERR_MSG.reference())));
            return out;
        };
    }

    public static RowMapper<Session> restSessionWithInstanceMapper() {
        return rs -> {
            RestSession out = createBaseRestSession(rs);
            out.setOs(rs.getString(OS.reference()));
            out.setRe(rs.getString(RE.reference()));
            out.setAddress(rs.getString(ADDRESS.reference()));
            out.setAppName(rs.getString(APP_NAME.reference()));
            setRequestMasks(out);
            return out;
        };
    }

    public static RowMapper<Session> restSessionWithoutInstanceMapper() {
        return rs -> {
            RestSession out = createBaseRestSession(rs);
            out.setInstanceId(rs.getString(INSTANCE_ENV.reference()));
            out.setCacheControl(rs.getString(CACHE_CONTROL.reference()));
            setRequestMasks(out);
            return out;
        };
    }

    private static RestSession createBaseRestSession(ResultSet rs) throws SQLException {
        RestSession out = new RestSession();
        out.setId(rs.getString(ID.reference()));
        out.setMethod(rs.getString(METHOD.reference()));
        out.setProtocol(rs.getString(PROTOCOL.reference()));
        out.setHost(rs.getString(HOST.reference()));
        out.setPort(rs.getInt(PORT.reference()));
        out.setPath(rs.getString(PATH.reference()));
        out.setQuery(rs.getString(QUERY.reference()));
        out.setContentType(rs.getString(MEDIA.reference()));
        out.setAuthScheme(rs.getString(AUTH.reference()));
        out.setStatus(rs.getInt(STATUS.reference()));
        out.setInDataSize(rs.getLong(SIZE_IN.reference()));
        out.setOutDataSize(rs.getLong(SIZE_OUT.reference()));
        out.setInContentEncoding(rs.getString(CONTENT_ENCODING_IN.reference()));
        out.setOutContentEncoding(rs.getString(CONTENT_ENCODING_OUT.reference()));
        out.setStart(fromNullableTimestamp(rs.getTimestamp(START.reference())));
        out.setEnd(fromNullableTimestamp(rs.getTimestamp(END.reference())));
        out.setThreadName(rs.getString(THREAD.reference()));
        out.setException(getExceptionInfoIfNotNull(rs.getString(ERR_TYPE.reference()), rs.getString(ERR_MSG.reference())));
        out.setName(rs.getString(API_NAME.reference()));
        out.setUserAgent(rs.getString(USER_AGT.reference()));
        out.setUser(rs.getString(USER.reference()));
        out.setMask(rs.getInt(MASK.reference()));
        out.setInstanceId(rs.getString(INSTANCE_ENV.reference()));
        out.setCacheControl(rs.getString(CACHE_CONTROL.reference()));
        return out;
    }

    private static void setRequestMasks(RestSession out) {
        if (RequestMask.JDBC.is(out.getMask())) {
            out.setDatabaseRequests(new ArrayList<>());
        }
        if (RequestMask.LOCAL.is(out.getMask())) {
            out.setLocalRequests(new ArrayList<>());
        }
        if (RequestMask.REST.is(out.getMask())) {
            out.setRestRequests(new ArrayList<>());
        }
        if (RequestMask.FTP.is(out.getMask())) {
            out.setFtpRequests(new ArrayList<>());
        }
        if (RequestMask.SMTP.is(out.getMask())) {
            out.setMailRequests(new ArrayList<>());
        }
        if (RequestMask.LDAP.is(out.getMask())) {
            out.setLdapRequests(new ArrayList<>());
        }
    }


}
