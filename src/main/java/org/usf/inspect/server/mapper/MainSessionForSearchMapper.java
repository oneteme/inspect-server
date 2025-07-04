package org.usf.inspect.server.mapper;

import static org.usf.inspect.server.Utils.fromNullableTimestamp;
import static org.usf.inspect.server.config.TraceApiColumn.APP_NAME;
import static org.usf.inspect.server.config.TraceApiColumn.END;
import static org.usf.inspect.server.config.TraceApiColumn.ERR_MSG;
import static org.usf.inspect.server.config.TraceApiColumn.ERR_TYPE;
import static org.usf.inspect.server.config.TraceApiColumn.ID;
import static org.usf.inspect.server.config.TraceApiColumn.LOCATION;
import static org.usf.inspect.server.config.TraceApiColumn.NAME;
import static org.usf.inspect.server.config.TraceApiColumn.START;
import static org.usf.inspect.server.config.TraceApiColumn.TYPE;
import static org.usf.inspect.server.config.TraceApiColumn.USER;
import static org.usf.inspect.server.service.RequestService.getExceptionInfoIfNotNull;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.usf.inspect.server.model.MainSession;
import org.usf.inspect.server.model.Session;
import org.usf.jquery.core.ResultSetMapper;


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