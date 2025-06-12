package org.usf.inspect.server.mapper;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.usf.inspect.core.InstanceType;
import org.usf.inspect.server.model.InstanceEnvironment;
import org.usf.inspect.server.model.RestRequest;
import org.usf.jquery.core.RowMapper;

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


}
