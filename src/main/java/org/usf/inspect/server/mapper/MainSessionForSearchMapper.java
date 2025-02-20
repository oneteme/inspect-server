package org.usf.inspect.server.mapper;

import org.usf.inspect.server.Constants;
import org.usf.inspect.server.exception.PayloadTooLargeException;
import org.usf.inspect.server.model.MainSession;
import org.usf.inspect.server.model.Session;
import org.usf.jquery.core.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.usf.inspect.server.config.TraceApiColumn.*;
import static org.usf.inspect.server.config.TraceApiColumn.ERR_MSG;
import static org.usf.inspect.server.service.RequestService.fromNullableTimestamp;
import static org.usf.inspect.server.service.RequestService.getExceptionInfoIfNotNull;


public class MainSessionForSearchMapper implements ResultSetMapper<List<Session>> {

    @Override
    public List<Session> map(ResultSet rs) throws SQLException {
        List<Session> sessions = new ArrayList<>();
        while (rs.next()) {
            MainSession main = new MainSession();
            main.setId(rs.getString(ID.reference())); // add value of nullable
            main.setName(rs.getString(NAME.reference()));
            main.setStart(fromNullableTimestamp(rs.getTimestamp(START.reference())));
            main.setEnd(fromNullableTimestamp(rs.getTimestamp(END.reference())));
            main.setLocation(rs.getString(LOCATION.reference()));
            main.setAppName(rs.getString(APP_NAME.reference()));
            main.setUser(rs.getString(USER.reference()));
            main.setType(rs.getString(TYPE.reference()));
            main.setException(getExceptionInfoIfNotNull(rs.getString(ERR_TYPE.reference()), rs.getString(ERR_MSG.reference())));
            sessions.add(main);
        }
        return sessions;
    }
}