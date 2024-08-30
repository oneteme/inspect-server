package org.usf.inspect.server.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.usf.inspect.core.*;
import org.usf.inspect.jdbc.SqlCommand;
import org.usf.inspect.server.config.TraceApiColumn;
import org.usf.inspect.server.config.TraceApiTable;
import org.usf.inspect.server.config.constant.JoinConstant;
import org.usf.inspect.server.dao.RequestDao;
import org.usf.inspect.server.model.*;
import org.usf.inspect.server.model.filter.JqueryMainSessionFilter;
import org.usf.inspect.server.model.filter.JqueryRequestSessionFilter;
import org.usf.inspect.server.model.wrapper.*;
import org.usf.jquery.core.DBFilter;
import org.usf.jquery.core.RequestQueryBuilder;
import org.usf.jquery.core.TaggableColumn;
import org.usf.jquery.web.ColumnDecorator;
import org.usf.jquery.web.ViewDecorator;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.usf.inspect.server.Utils.requireSingle;
import static org.usf.inspect.server.config.TraceApiColumn.*;
import static org.usf.inspect.server.config.TraceApiTable.*;
import static org.usf.inspect.server.config.constant.JoinConstant.*;

@Service
@RequiredArgsConstructor
public class RequestService {

    private final DataSource ds;
    private final RequestDao dao;

    public void addInstance(ServerInstanceEnvironment instance) {
        dao.saveInstanceEnvironment(instance);
    }

    public void addSessions(List<ServerSession> sessions) {
        dao.saveSessions(sessions);
    }

    public Session getMainTree(String id) throws SQLException {
        List<String> prntIds = dao.selectChildsById(id);
        List<Session> prntIncList = getRestSessionsForTree(prntIds);
        Session session = getMainSessionForTree(id);
        if(session != null){
            prntIncList.add(session);
        }
        createTree(prntIncList);
        return prntIncList.stream().filter(r ->  r.getId().equals(id)).findFirst().orElseThrow();
    }

    public Session getRestTree(String id) throws SQLException {
        List<String> prntIds = dao.selectChildsById(id);
        List<Session> prntIncList = getRestSessionsForTree(prntIds);
        createTree(prntIncList);
        return prntIncList.stream().filter(r ->  r.getId().equals(id)).findFirst().orElseThrow();
    }

    private void createTree(List<Session> sessions) {
        sessions.forEach(prntA ->
                sessions.forEach(prntB -> {
                    if (!Objects.equals(prntA.getId(), prntB.getId())){
                        Optional<RestRequest> opt = prntB.getRestRequests().stream()
                                .filter(k -> prntA.getId().equals(k.getId()))
                                .findFirst();
                        if (opt.isPresent()) {
                            var ex = (Exchange) opt.get();
                            ex.setRemoteTrace((RestSession) prntA);
                        }
                    }
                })
        );
    }

    public Map<String,String> getSessionParent(String childId) throws SQLException{
        var prnt = getPropertyByFilters(REST_REQUEST, PARENT, REST_REQUEST.column(REMOTE).eq(childId));
        if(prnt != null){
            var res = getPropertyByFilters(REST_SESSION, ID, REST_SESSION.column(ID).eq(prnt));
            if(res != null) {
                return Map.of("id", res, "type", "rest");
            }
            res = getPropertyByFilters(MAIN_SESSION, ID, MAIN_SESSION.column(ID).eq(prnt));
            if(res!= null){
                var type = getPropertyByFilters(MAIN_SESSION, TYPE, MAIN_SESSION.column(ID).eq(res));
                return Map.of("id", res, "type", type);
            }
        }
        return Collections.emptyMap();
    }

    public ServerInstanceEnvironment getInstance(String id) throws SQLException {
        var v = new RequestQueryBuilder()
                .columns(
                        getColumns(
                                INSTANCE, ID, USER, TYPE, START, APP_NAME, VERSION, ADDRESS,
                                ENVIRONEMENT, OS, RE, COLLECTOR
                        ))
                .filters(INSTANCE.column(ID).eq(id));
        return v.build().execute(ds, rs -> {
            if(rs.next()) {
                return new ServerInstanceEnvironment(
                        rs.getString(ID.reference()),
                        rs.getString(APP_NAME.reference()),
                        rs.getString(VERSION.reference()),
                        rs.getString(ADDRESS.reference()),
                        rs.getString(ENVIRONEMENT.reference()),
                        rs.getString(OS.reference()),
                        rs.getString(RE.reference()),
                        rs.getString(USER.reference()),
                        InstanceType.valueOf(rs.getString(TYPE.reference())),
                        fromNullableTimestamp(rs.getTimestamp(START.reference())),
                        rs.getString(COLLECTOR.reference()));
            }
            return null;
        });
    }

    public Session getRestSession(String id) throws SQLException{
        JqueryRequestSessionFilter jsf = new JqueryRequestSessionFilter(Collections.singletonList(id).toArray(String[]::new));
        return requireSingle(getRestSessions(jsf));
    }

    public List<Session> getRestSessionsForTree(List<String> ids) throws SQLException {
        JqueryRequestSessionFilter jsf = new JqueryRequestSessionFilter(ids.toArray(String[]::new));
        List<Session> sessions = getRestSessions(jsf);
        if (!sessions.isEmpty()) {
            var reqMap = sessions.stream().collect(toMap(Session::getId, identity()));
            var parentIds = reqMap.keySet().stream().toList();
            getRestRequests(parentIds, Exchange::new).forEach(r -> reqMap.get(r.getCdSession()).append(r.getRequest()));
            getLocalRequests(parentIds).forEach(r -> reqMap.get(r.getCdSession()).append(r.getStage()));
            getDatabaseRequests(parentIds).forEach(q -> reqMap.get(q.getCdSession()).append(q.getDatabaseRequest()));
        }
        return sessions;
    }

    public List<Session> getRestSessions(JqueryRequestSessionFilter jsf) throws SQLException {
        var v = new RequestQueryBuilder()
                .columns(
                    getColumns(
                            REST_SESSION, ID, API_NAME, METHOD,
                            PROTOCOL, HOST, PORT, PATH, QUERY, MEDIA, AUTH, STATUS, SIZE_IN, SIZE_OUT, CONTENT_ENCODING_IN, CONTENT_ENCODING_OUT,
                            START, END, THREAD, ERR_TYPE, ERR_MSG, USER_AGT, MASK, USER, CACHE_CONTROL, INSTANCE_ENV
                    ))
                .columns(getColumns(INSTANCE, APP_NAME))
                .filters(REST_SESSION.column(INSTANCE_ENV).eq(INSTANCE.column(ID)));
        if(jsf != null) {
            v.filters(jsf.filters(REST_SESSION).toArray(DBFilter[]::new));
        }
        return v.build().execute(ds, rs -> {
            List<Session> sessions = new ArrayList<>();
            while (rs.next()) {
                ServerRestSession session = new ServerRestSession();
                session.setId(rs.getString(ID.reference()));
                session.setMethod(rs.getString(METHOD.reference()));
                session.setProtocol(rs.getString(PROTOCOL.reference()));
                session.setHost(rs.getString(HOST.reference()));
                session.setPort(rs.getInt(PORT.reference()));
                session.setPath(rs.getString(PATH.reference()));
                session.setQuery(rs.getString(QUERY.reference()));
                session.setContentType((rs.getString(MEDIA.reference())));
                session.setAuthScheme((rs.getString(AUTH.reference())));
                session.setStatus(rs.getInt(STATUS.reference()));
                session.setInDataSize(rs.getLong(SIZE_IN.reference()));
                session.setOutDataSize(rs.getLong(SIZE_OUT.reference()));
                session.setInContentEncoding(rs.getString(CONTENT_ENCODING_IN.reference()));
                session.setOutContentEncoding(rs.getString(CONTENT_ENCODING_OUT.reference()));
                session.setStart(fromNullableTimestamp(rs.getTimestamp(START.reference())));
                session.setEnd(fromNullableTimestamp(rs.getTimestamp(END.reference())));
                session.setThreadName(rs.getString(THREAD.reference()));
                session.setException(getExceptionInfoIfNotNull(rs.getString(ERR_TYPE.reference()), rs.getString(ERR_MSG.reference())));
                session.setName(rs.getString(API_NAME.reference()));
                session.setUserAgent(rs.getString(USER_AGT.reference()));
                session.setUser(rs.getString(USER.reference()));
                session.setInstanceId(rs.getString(INSTANCE_ENV.reference()));
                session.setAppName(rs.getString(APP_NAME.reference()));
                session.setCacheControl(rs.getString(CACHE_CONTROL.reference()));
                session.setMask(rs.getInt(MASK.reference()));
                session.setRestRequests(new ArrayList<>());
                session.setLocalRequests(new ArrayList<>());
                session.setDatabaseRequests(new ArrayList<>());
                sessions.add(session);
            }
            return sessions;
        });
    }

    public Session getMainSessionForTree(String id) throws SQLException {
        JqueryMainSessionFilter jsf = new JqueryMainSessionFilter(Collections.singletonList(id).toArray(String[]::new));
        Session session = requireSingle(getMainSessions(jsf));
        if (session != null) {
            getRestRequests(session.getId(), Exchange::new).forEach(r -> session.append(r.getRequest()));
            getLocalRequests(session.getId()).forEach(r -> session.append(r.getStage()));
            getDatabaseRequests(session.getId()).forEach(d -> session.append(d.getDatabaseRequest()));
        }
        return session;
    }

    public Session getMainSession(String id) throws SQLException{
        JqueryMainSessionFilter jsf = new JqueryMainSessionFilter(Collections.singletonList(id).toArray(String[]::new));
        return requireSingle(getMainSessions(jsf));
    }

    public List<Session> getMainSessions(JqueryMainSessionFilter jsf) throws SQLException {
        var v = new RequestQueryBuilder()
                .columns(
                    getColumns(
                            MAIN_SESSION, ID, NAME, START, END, TYPE, LOCATION, THREAD,
                            ERR_TYPE, ERR_MSG, MASK, USER, INSTANCE_ENV
                    ))
                .columns(getColumns(INSTANCE, APP_NAME))
                .filters(MAIN_SESSION.column(INSTANCE_ENV).eq(INSTANCE.column(ID)));;
        if(jsf != null) {
            v.filters(jsf.filters(MAIN_SESSION).toArray(DBFilter[]::new));
        }
        return v.build().execute(ds, rs -> {
            List<Session> sessions = new ArrayList<>();
            while(rs.next()) {
                ServerMainSession main = new ServerMainSession();
                main.setId(rs.getString(ID.reference())); // add value of nullable
                main.setName(rs.getString(NAME.reference()));
                main.setStart(fromNullableTimestamp(rs.getTimestamp(START.reference())));
                main.setEnd(fromNullableTimestamp(rs.getTimestamp(END.reference())));
                main.setType(rs.getString(TYPE.reference()));
                main.setLocation(rs.getString(LOCATION.reference()));
                main.setThreadName(rs.getString(THREAD.reference()));
                main.setException(getExceptionInfoIfNotNull(rs.getString(ERR_TYPE.reference()), rs.getString(ERR_MSG.reference())));
                main.setAppName(rs.getString(APP_NAME.reference()));
                main.setUser(rs.getString(USER.reference()));
                main.setInstanceId(rs.getString(INSTANCE_ENV.reference()));
                main.setRestRequests(new ArrayList<>());
                main.setLocalRequests(new ArrayList<>());
                main.setDatabaseRequests(new ArrayList<>());
                main.setMask(rs.getInt(MASK.reference()));
                sessions.add(main);
            }
            return sessions;
        });
    }

    public List<RestRequestWrapper> getRestRequests(String cdSession, Supplier<? extends RestRequest> fn) throws SQLException {
        return getRestRequests(Collections.singletonList(cdSession), fn);
    }

    private List<RestRequestWrapper> getRestRequests(List<String> cdSessions, Supplier<? extends RestRequest> fn) throws SQLException { //use criteria
        var v = new RequestQueryBuilder()
                .columns(getColumns(
                        REST_REQUEST, ID, PROTOCOL, HOST, PORT, PATH, QUERY, METHOD, STATUS, SIZE_IN,
                        SIZE_OUT, CONTENT_ENCODING_IN, CONTENT_ENCODING_OUT, START, END, THREAD, REMOTE, PARENT
                ))
                .columns(getColumns(EXCEPTION, ERR_TYPE, ERR_MSG))
                //.columns(REST_REQUEST.column(PARENT).as("test"), EXCEPTION.column(PARENT).as("test2"))
                .joins(REST_REQUEST.join(EXCEPTION_JOIN).build())
                .filters(REST_REQUEST.column(PARENT).in(cdSessions.toArray()))
                .orders(REST_REQUEST.column(START).order());
        return v.build().execute(ds, rs -> {
            List<RestRequestWrapper> outs = new ArrayList<>();
            while (rs.next()) {
                RestRequestWrapper out = new RestRequestWrapper(rs.getString(PARENT.reference()), fn);
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
                out.setException(new ExceptionInfo(rs.getString(ERR_TYPE.reference()), rs.getString(ERR_MSG.reference())));
                outs.add(out);
            }
            return outs;
        });
    }

    public List<LocalRequestWrapper> getLocalRequests(String cdSession) throws SQLException{
        return getLocalRequests(Collections.singletonList(cdSession));
    }

    private List<LocalRequestWrapper> getLocalRequests(List<String> cdSessions) throws SQLException{
        var v = new RequestQueryBuilder()
                .columns(getColumns(
                        LOCAL_REQUEST, ID, NAME, LOCATION, START, END, USER, THREAD, PARENT
                ))
                .columns(getColumns(EXCEPTION, ERR_TYPE, ERR_MSG))
                .joins(LOCAL_REQUEST.join(EXCEPTION_JOIN).build())
                .filters(LOCAL_REQUEST.column(PARENT).in(cdSessions.toArray())).orders(LOCAL_REQUEST.column(START).order());
        return v.build().execute(ds, rs -> {
            List<LocalRequestWrapper> outs = new ArrayList<>();
            while (rs.next()) {
                LocalRequestWrapper out = new LocalRequestWrapper(rs.getString(PARENT.reference()));
                out.setId(rs.getLong(ID.reference()));
                out.setName(rs.getString(NAME.reference()));
                out.setLocation(rs.getString(LOCATION.reference()));
                out.setStart(fromNullableTimestamp(rs.getTimestamp(START.reference())));
                out.setEnd(fromNullableTimestamp(rs.getTimestamp(END.reference())));
                out.setUser(rs.getString(USER.reference()));
                out.setThreadName(rs.getString(THREAD.reference()));
                out.setException(new ExceptionInfo(rs.getString(ERR_TYPE.reference()), rs.getString(ERR_MSG.reference())));
                outs.add(out);
            }
            return outs;
        });
    }


    public DatabaseRequestWrapper getDatabaseRequest(long idDatabase) throws SQLException {
        return requireSingle(getDatabaseRequests(DATABASE_REQUEST.column(ID).eq(idDatabase)));
    }

    public List<DatabaseRequestWrapper> getDatabaseRequests(List<String> cdSession) throws SQLException {
        return getDatabaseRequests(DATABASE_REQUEST.column(PARENT).in(cdSession.toArray()));
    }

    public List<DatabaseRequestWrapper> getDatabaseRequests(String cdSession) throws SQLException {
        return getDatabaseRequests(DATABASE_REQUEST.column(PARENT).eq(cdSession));
    }

    private List<DatabaseRequestWrapper> getDatabaseRequests(DBFilter filter) throws SQLException {
        var v = new RequestQueryBuilder()
                .columns(
                    getColumns(
                            DATABASE_REQUEST, ID, HOST, PORT, DB, START, END, USER, THREAD, DRIVER,
                            DB_NAME, DB_VERSION, COMMANDS, COMPLETE, PARENT
                    ))
                .columns(getColumns(EXCEPTION, ERR_TYPE, ERR_MSG))
                .joins(DATABASE_REQUEST.join(EXCEPTION_JOIN).build())
                .filters(filter)
                .orders(DATABASE_REQUEST.column(START).order());
        return v.build().execute(ds, rs -> {
            List<DatabaseRequestWrapper> outs = new ArrayList<>();
            while (rs.next()) {
                DatabaseRequestWrapper out = new DatabaseRequestWrapper(rs.getString(PARENT.reference()));
                out.setId(rs.getLong(ID.reference()));
                out.setHost(rs.getString(HOST.reference()));
                out.setPort(rs.getInt(PORT.reference()));
                out.setName(rs.getString(DB.reference()));
                out.setStart(fromNullableTimestamp(rs.getTimestamp(START.reference())));
                out.setEnd(fromNullableTimestamp(rs.getTimestamp(END.reference())));
                out.setUser(rs.getString(USER.reference()));
                out.setThreadName(rs.getString(THREAD.reference()));
                out.setDriverVersion(rs.getString(DRIVER.reference()));
                out.setProductName(rs.getString(DB_NAME.reference()));
                out.setProductVersion(rs.getString(DB_VERSION.reference()));
                out.setActions(new ArrayList<>());
                out.setCommands(valueOfNullabletoEnumList(SqlCommand.class, rs.getString(COMMANDS.reference())));
                out.setCompleted(getExceptionInfoIfNotNull(rs.getString(ERR_TYPE.reference()), rs.getString(ERR_MSG.reference())) == null);
                outs.add(out);
            }
            return outs;
        });
    }

    public List<DatabaseRequestStageWrapper> getDatabaseRequestStages(Long id) throws SQLException {
        var v = new RequestQueryBuilder()
                .columns(
                        getColumns(
                                DATABASE_STAGE, NAME, START, END, ACTION_COUNT, ORDER, PARENT
                        ))
                .columns(getColumns(EXCEPTION, ERR_TYPE, ERR_MSG))
                .joins(DATABASE_STAGE.join(EXCEPTION_JOIN).build())
                .filters(DATABASE_STAGE.column(PARENT).eq(id))
                .orders(DATABASE_STAGE.column(START).order());
        return v.build().execute(ds, rs -> {
            List<DatabaseRequestStageWrapper> actions = new ArrayList<>();
            while (rs.next()) {
                var action = new DatabaseRequestStageWrapper(
                        rs.getLong(PARENT.reference()),
                        rs.getLong(ORDER.reference()),
                        new DatabaseRequestStage()
                );
                action.setName(rs.getString(NAME.reference()));
                action.setStart(fromNullableTimestamp(rs.getTimestamp(START.reference())));
                action.setEnd(fromNullableTimestamp(rs.getTimestamp(END.reference())));
                action.setCount(ofNullable(rs.getString(ACTION_COUNT.reference())).map(str -> Arrays.stream(str.split(",")).mapToLong(Long::parseLong).toArray()).orElse(null));
                action.setException(new ExceptionInfo(rs.getString(ERR_TYPE.reference()), rs.getString(ERR_MSG.reference())));
                actions.add(action);
            }
            return actions;
        });
    }

    public FtpRequestWrapper getFtpRequest(long id) throws SQLException {
        return requireSingle(getFtpRequests(FTP_REQUEST.column(ID).eq(id)));
    }

    public List<FtpRequestWrapper> getFtpRequests(String cdSession) throws SQLException {
        return getFtpRequests(FTP_REQUEST.column(PARENT).eq(cdSession));
    }

    private List<FtpRequestWrapper> getFtpRequests(DBFilter filter) throws SQLException {
        var v = new RequestQueryBuilder()
                .columns(
                    getColumns(
                            FTP_REQUEST, ID, HOST, PORT, PROTOCOL, SERVER_VERSION, CLIENT_VERSION, START, END, USER, THREAD, PARENT
                    )
                )
                .columns(getColumns(EXCEPTION, ERR_TYPE, ERR_MSG))
                .joins(FTP_REQUEST.join(EXCEPTION_JOIN).build())
                .filters(filter)
                .orders(FTP_REQUEST.column(START).order());
        return v.build().execute(ds, rs -> {
            List<FtpRequestWrapper> outs = new ArrayList<>();
            while (rs.next()) {
                FtpRequestWrapper out = new FtpRequestWrapper(rs.getString(PARENT.reference()), new FtpRequest());
                out.setId(rs.getLong(ID.reference()));
                out.setHost(rs.getString(HOST.reference()));
                out.setPort(rs.getInt(PORT.reference()));
                out.setProtocol(rs.getString(PROTOCOL.reference()));
                out.setServerVersion(rs.getString(SERVER_VERSION.reference()));
                out.setClientVersion(rs.getString(CLIENT_VERSION.reference()));
                out.setStart(fromNullableTimestamp(rs.getTimestamp(START.reference())));
                out.setEnd(fromNullableTimestamp(rs.getTimestamp(END.reference())));
                out.setUser(rs.getString(USER.reference()));
                out.setThreadName(rs.getString(THREAD.reference()));
                out.setActions(new ArrayList<>());
                outs.add(out);
            }
            return outs;
        });
    }

    public List<FtpRequestStageWrapper> getFtpRequestStages(long id) throws SQLException {
        var v = new RequestQueryBuilder()
                .columns(
                    getColumns(
                            FTP_STAGE, NAME, START, END, ARG, ORDER, PARENT
                    ))
                .columns(getColumns(EXCEPTION, ERR_TYPE, ERR_MSG))
                .joins(FTP_STAGE.join(EXCEPTION_JOIN).build())
                .filters(FTP_STAGE.column(PARENT).eq(id))
                .orders(FTP_STAGE.column(ORDER).order());
        return v.build().execute(ds, rs -> {
            List<FtpRequestStageWrapper> actions = new ArrayList<>();
            while (rs.next()) {
                var action = new FtpRequestStageWrapper(
                        rs.getLong(PARENT.reference()),
                        rs.getLong(ORDER.reference()),
                        new FtpRequestStage()
                );
                action.setName(rs.getString(NAME.reference()));
                action.setStart(fromNullableTimestamp(rs.getTimestamp(START.reference())));
                action.setEnd(fromNullableTimestamp(rs.getTimestamp(END.reference())));
                action.setArgs(ofNullable(rs.getString(ARG.reference())).map(str -> Arrays.stream(str.split(",")).toArray(String[]::new)).orElse(null));
                action.setException(new ExceptionInfo(rs.getString(ERR_TYPE.reference()), rs.getString(ERR_MSG.reference())));
                actions.add(action);
            }
            return actions;
        });
    }

    public SmtpRequestWrapper getSmtpRequest(long id) throws SQLException {
        return requireSingle(getSmtpRequests(SMTP_REQUEST.column(ID).eq(id)));
    }

    public List<SmtpRequestWrapper> getSmtpRequests(String cdSession) throws SQLException {
        return getSmtpRequests(SMTP_REQUEST.column(PARENT).in(cdSession));
    }

    private List<SmtpRequestWrapper> getSmtpRequests(DBFilter filter) throws SQLException {
        var v = new RequestQueryBuilder()
                .columns(
                    getColumns(
                            SMTP_REQUEST, ID, HOST, PORT, START, END, USER, THREAD, PARENT
                    ))
                .filters(filter)
                .orders(SMTP_REQUEST.column(START).order());
        return v.build().execute(ds, rs -> {
            List<SmtpRequestWrapper> outs = new ArrayList<>();
            while (rs.next()) {
                SmtpRequestWrapper out = new SmtpRequestWrapper(rs.getString(PARENT.reference()), new MailRequest());
                out.setId(rs.getLong(ID.reference()));
                out.setHost(rs.getString(HOST.reference()));
                out.setPort(rs.getInt(PORT.reference()));
                out.setStart(fromNullableTimestamp(rs.getTimestamp(START.reference())));
                out.setEnd(fromNullableTimestamp(rs.getTimestamp(END.reference())));
                out.setUser(rs.getString(USER.reference()));
                out.setThreadName(rs.getString(THREAD.reference()));
                out.setActions(new ArrayList<>());
                outs.add(out);
            }
            return outs;
        });
    }

    public List<SmtpRequestStageWrapper> getSmtpRequestStages(long id) throws SQLException {
        var v = new RequestQueryBuilder()
                .columns(
                    getColumns(SMTP_STAGE, NAME, START, END, ORDER, PARENT)
                )
                .columns(getColumns(EXCEPTION, ERR_TYPE, ERR_MSG))
                .joins(SMTP_STAGE.join(EXCEPTION_JOIN).build())
                .filters(SMTP_STAGE.column(PARENT).eq(id))
                .orders(SMTP_STAGE.column(ORDER).order());
        return v.build().execute(ds, rs -> {
            List<SmtpRequestStageWrapper> actions = new ArrayList<>();
            while (rs.next()) {
                var action = new SmtpRequestStageWrapper(
                        rs.getLong(PARENT.reference()),
                        rs.getLong(ORDER.reference()),
                        new MailRequestStage()
                );
                action.setName(rs.getString(NAME.reference()));
                action.setStart(fromNullableTimestamp(rs.getTimestamp(START.reference())));
                action.setEnd(fromNullableTimestamp(rs.getTimestamp(END.reference())));
                action.setException(new ExceptionInfo(rs.getString(ERR_TYPE.reference()), rs.getString(ERR_MSG.reference())));
                actions.add(action);
            }
            return actions;
        });
    }

    public List<SmtpRequestMailWrapper> getSmtpRequestMails(long id) throws SQLException {
        var v = new RequestQueryBuilder()
                .columns(
                    getColumns(SMTP_MAIL, SUBJECT, FROM, RECIPIENTS, MEDIA, REPLY_TO, SIZE, PARENT)
                )
                .filters(SMTP_MAIL.column(PARENT).eq(id));
        return v.build().execute(ds, rs -> {
            List<SmtpRequestMailWrapper> mails = new ArrayList<>();
            while (rs.next()) {
                var mail = new SmtpRequestMailWrapper(
                        rs.getLong(PARENT.reference()),
                        new Mail()
                );
                mail.setContentType(rs.getString(MEDIA.reference()));
                mail.setFrom(ofNullable(rs.getString(FROM.reference())).map(str -> Arrays.stream(str.split(",")).toArray(String[]::new)).orElse(null));
                mail.setRecipients(ofNullable(rs.getString(RECIPIENTS.reference())).map(str -> Arrays.stream(str.split(",")).toArray(String[]::new)).orElse(null));
                mail.setReplyTo(ofNullable(rs.getString(REPLY_TO.reference())).map(str -> Arrays.stream(str.split(",")).toArray(String[]::new)).orElse(null));
                mail.setSize(rs.getInt(SIZE.reference()));
                mail.setSubject(rs.getString(SUBJECT.reference()));
                mails.add(mail);
            }
            return mails;
        });
    }

    public LdapRequestWrapper getLdapRequest(long id) throws SQLException {
        return requireSingle(getLdapRequests(LDAP_REQUEST.column(ID).eq(id)));
    }

    public List<LdapRequestWrapper> getLdapRequests(String cdSession) throws SQLException {
        return getLdapRequests(LDAP_REQUEST.column(PARENT).in(cdSession));
    }

    private List<LdapRequestWrapper> getLdapRequests(DBFilter filter) throws SQLException {
        var v = new RequestQueryBuilder()
                .columns(
                    getColumns(
                            LDAP_REQUEST, ID, HOST, PORT, PROTOCOL, START, END, USER, THREAD, PARENT
                    ))
                .filters(filter)
                .orders(LDAP_REQUEST.column(START).order());
        return v.build().execute(ds, rs -> {
            List<LdapRequestWrapper> outs = new ArrayList<>();
            while (rs.next()) {
                LdapRequestWrapper out = new LdapRequestWrapper(rs.getString(PARENT.reference()), new NamingRequest());
                out.setId(rs.getLong(ID.reference()));
                out.setHost(rs.getString(HOST.reference()));
                out.setPort(rs.getInt(PORT.reference()));
                out.setProtocol(rs.getString(PROTOCOL.reference()));
                out.setStart(fromNullableTimestamp(rs.getTimestamp(START.reference())));
                out.setEnd(fromNullableTimestamp(rs.getTimestamp(END.reference())));
                out.setUser(rs.getString(USER.reference()));
                out.setThreadName(rs.getString(THREAD.reference()));
                out.setActions(new ArrayList<>());
                outs.add(out);
            }
            return outs;
        });
    }

    public List<LdapRequestStageWrapper> getLdapRequestStages(Long id) throws SQLException {
        var v = new RequestQueryBuilder()
                .columns(
                    getColumns(
                            LDAP_STAGE, NAME, START, END, ARG, ORDER, PARENT
                    ))
                .columns(getColumns(EXCEPTION, ERR_TYPE, ERR_MSG))
                .joins(LDAP_REQUEST.join(EXCEPTION_JOIN).build())
                .filters(LDAP_STAGE.column(PARENT).eq(id))
                .orders(LDAP_STAGE.column(ORDER).order());
        return v.build().execute(ds, rs -> {
            List<LdapRequestStageWrapper> actions = new ArrayList<>();
            while (rs.next()) {
                var action = new LdapRequestStageWrapper(
                        rs.getLong(PARENT.reference()),
                        rs.getLong(ORDER.reference()),
                        new NamingRequestStage()
                );
                action.setName(rs.getString(NAME.reference()));
                action.setStart(fromNullableTimestamp(rs.getTimestamp(START.reference())));
                action.setEnd(fromNullableTimestamp(rs.getTimestamp(END.reference())));
                action.setException(new ExceptionInfo(rs.getString(ERR_TYPE.reference()), rs.getString(ERR_MSG.reference())));
                actions.add(action);
            }
            return actions;
        });
    }

    private String getPropertyByFilters(TraceApiTable table, TraceApiColumn target, DBFilter filters) throws SQLException { // main / apissesion
        var v = new RequestQueryBuilder().columns(getColumns(table,target)).filters(filters);
        return v.build().execute(ds, rs -> {
            if(rs.next()){
                return rs.getString(target.reference()); // to be changed
            }
            return null;
        });
    }

    private static TaggableColumn[] getColumns(ViewDecorator table, ColumnDecorator... columns) {
        return Stream.of(columns).map(table::column).toArray(TaggableColumn[]::new);
    }

    private static Instant fromNullableTimestamp(Timestamp timestamp) {
        return ofNullable(timestamp).map(Timestamp::toInstant).orElse(null);
    }

    private static ExceptionInfo getExceptionInfoIfNotNull(String className, String message) {
        if(className != null || message != null) {
            return new ExceptionInfo(className, message);
        }
        return null;
    }

    private static <T extends Enum<T>> T valueOfNullable(Class<T> classe, String value) {
        return ofNullable(value)
                .flatMap(v -> Stream.of(classe.getEnumConstants()).filter(e -> e.name().equals(v)).findAny())
                .orElse(null);
    }

    private static <T extends Enum<T>> List<T> valueOfNullabletoEnumList(Class<T> classe, String values){
        return Stream.of(splitNullable(values))
                .map(v-> valueOfNullable(classe, v))
                .filter(Objects::nonNull)
                .toList();
    }

    private static final String[] empty_array = new String[0];

    private static String[] splitNullable(String s){
        return isNull(s) ? empty_array : s.split(",");
    }
}
