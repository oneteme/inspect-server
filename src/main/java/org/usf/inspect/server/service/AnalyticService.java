package org.usf.inspect.server.service;

import static java.sql.Timestamp.from;
import static org.usf.inspect.server.Utils.fromNullableTimestamp;
import static org.usf.inspect.server.config.TraceApiColumn.END;
import static org.usf.inspect.server.config.TraceApiColumn.ID;
import static org.usf.inspect.server.config.TraceApiColumn.LOCATION;
import static org.usf.inspect.server.config.TraceApiColumn.NAME;
import static org.usf.inspect.server.config.TraceApiColumn.NODE_NAME;
import static org.usf.inspect.server.config.TraceApiColumn.PARENT;
import static org.usf.inspect.server.config.TraceApiColumn.START;
import static org.usf.inspect.server.config.TraceApiColumn.TYPE;
import static org.usf.inspect.server.config.TraceApiColumn.USER;
import static org.usf.inspect.server.config.TraceApiTable.MAIN_SESSION;
import static org.usf.inspect.server.config.TraceApiTable.USER_ACTION;
import static org.usf.inspect.server.config.constant.JoinConstant.USER_ACTION_JOIN;

import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.stereotype.Service;
import org.usf.inspect.server.model.MainSession;
import org.usf.inspect.server.model.UserAction;
import org.usf.jquery.core.QueryComposer;
import org.usf.jquery.core.ResultSetMapper;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AnalyticService {
    private final DataSource ds;

    private final static ResultSetMapper<List<UserAction>> RESULT_SET_MAPPER =  rs -> {
        List<UserAction> actions = new ArrayList<>();
        while (rs.next()) {
            var action = new UserAction(
                    rs.getString(NAME.reference()),
                    rs.getString(NODE_NAME.reference()),
                    rs.getString(TYPE.reference()),
                    fromNullableTimestamp(rs.getTimestamp(START.reference()))
            );
            action.setCdSession(rs.getString(PARENT.reference()));
            actions.add(action);
        }
        return actions;
    };

    public List<UserAction> getUserActions(@NonNull String idSession) throws SQLException {
        var q = new QueryComposer()
                .columns(
                        USER_ACTION.column(NAME),
                        USER_ACTION.column(NODE_NAME),
                        USER_ACTION.column(TYPE),
                        USER_ACTION.column(START),
                        USER_ACTION.column(PARENT)
                )
                .filters(USER_ACTION.column(PARENT).eq(idSession))
                .orders(USER_ACTION.column(START).asc());
        return q.build().execute(ds, RESULT_SET_MAPPER);
    }

    public List<MainSession> getUserActions(@NonNull String user, @NonNull Instant date, @NonNull Integer offSet, @NonNull Integer limit) throws SQLException {
        var q = new QueryComposer()
                .columns(
                        MAIN_SESSION.column(ID),
                        MAIN_SESSION.column(START).as("session_start"),
                        MAIN_SESSION.column(END),
                        MAIN_SESSION.column(LOCATION),
                        MAIN_SESSION.column(NAME).as("session_name")
                )
                .columns(
                        USER_ACTION.column(NAME).as("action_name"),
                        USER_ACTION.column(NODE_NAME),
                        USER_ACTION.column(TYPE),
                        USER_ACTION.column(START).as("action_start")
                )
                .joins(MAIN_SESSION.join(USER_ACTION_JOIN))
                .filters(MAIN_SESSION.column(USER).eq(user))
                .filters(MAIN_SESSION.column(START).ge(from(date)))
                .offset(offSet)
                .limit(limit)
                .orders(MAIN_SESSION.column(START).asc(), USER_ACTION.column(START).asc());


        return q.build().execute(ds, rs -> {
            List<MainSession> sessions = new ArrayList<>();
            while (rs.next()) {
                var userAction =  new UserAction(
                        rs.getString("action_name"),
                        rs.getString(NODE_NAME.reference()),
                        rs.getString(TYPE.reference()),
                        fromNullableTimestamp(rs.getTimestamp("action_start"))
                );
                var cdSession = rs.getString(ID.reference());
                var session = sessions.stream().filter(s -> s.getId().equals(cdSession)).findFirst().orElse(null);
                if(session == null) {
                    session = new MainSession();
                    session.setId(rs.getString(ID.reference()));
                    session.setStart(fromNullableTimestamp(rs.getTimestamp("session_start")));
                    session.setEnd(fromNullableTimestamp(rs.getTimestamp(END.reference())));
                    session.setName(rs.getString("session_name"));
                    session.setLocation(rs.getString(LOCATION.reference()));
                    if(userAction.getStart() == null) {
                        session.setUserActions(new ArrayList<>());
                    } else {
                        session.setUserActions(new ArrayList<>(List.of(userAction)));
                    }

                    sessions.add(session);
                } else {
                    session.getUserActions().add(userAction);
                }
            }
            return sessions;
        });
    }
}
