package org.usf.inspect.server.mapper;

import org.usf.inspect.server.Utils;
import org.usf.inspect.server.model.RestSession;
import org.usf.inspect.server.model.Session;
import org.usf.jquery.core.ResultSetMapper;


import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.usf.inspect.server.Utils.*;
import static org.usf.inspect.server.config.TraceApiColumn.*;
import static org.usf.inspect.server.service.RequestService.getExceptionInfoIfNotNull;

public class RestSessionForSearchMapper implements ResultSetMapper<List<Session>> {

    @Override
    public List<Session> map(ResultSet rs) throws SQLException {
        List<Session> sessions = new ArrayList<>();
        while (rs.next()) {
            RestSession session = new RestSession();
            session.setId(rs.getString(ID.reference()));
            session.setMethod(rs.getString(METHOD.reference()));
            session.setProtocol(rs.getString(PROTOCOL.reference()));
            session.setPath(rs.getString(PATH.reference()));
            session.setQuery(rs.getString(QUERY.reference()));
            session.setStatus(rs.getInt(STATUS.reference()));
            session.setInDataSize(rs.getLong(SIZE_IN.reference()));
            session.setOutDataSize(rs.getLong(SIZE_OUT.reference()));
            session.setStart(fromNullableTimestamp(rs.getTimestamp(START.reference())));
            session.setEnd(fromNullableTimestamp(rs.getTimestamp(END.reference())));
            session.setName(rs.getString(API_NAME.reference()));
            session.setUser(rs.getString(USER.reference()));
            session.setAppName(rs.getString(APP_NAME.reference()));
            session.setException(getExceptionInfoIfNotNull(rs.getString(ERR_TYPE.reference()), rs.getString(ERR_MSG.reference())));
            sessions.add(session);
        }
        return sessions;
    }


}
