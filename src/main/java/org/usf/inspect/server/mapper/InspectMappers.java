package org.usf.inspect.server.mapper;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.usf.inspect.core.InstanceType;
import org.usf.inspect.jdbc.SqlCommand;
import org.usf.inspect.server.RequestMask;
import org.usf.inspect.server.model.*;
import org.usf.jquery.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static java.util.Optional.ofNullable;
import static org.usf.inspect.server.Utils.fromNullableTimestamp;
import static org.usf.inspect.server.config.TraceApiColumn.*;
import static org.usf.inspect.server.config.TraceApiColumn.ID;
import static org.usf.inspect.server.service.RequestService.getExceptionInfoIfNotNull;
import static org.usf.inspect.server.service.RequestService.valueOfNullabletoEnumList;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class InspectMappers {

    public static InstanceEnvironment instanceEnvironmentMapper(ResultSet rs) throws SQLException {
        if(rs.next()) {
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
        }
        return null;
    }
    public static RowMapper<RestRequest> restRequestLazyMapper() {
       return InspectMappers::createBaseRestRequest;
    }

    public static RestRequest createBaseRestRequest(ResultSet rs) throws SQLException {
        RestRequest out = new RestRequest();
        out.setIdRequest(rs.getString(ID.reference()));
        out.setId(rs.getString(REMOTE.reference()));
        out.setProtocol(rs.getString(PROTOCOL.reference()));
        out.setHost(rs.getString(HOST.reference()));
        out.setPath(rs.getString(PATH.reference()));
        out.setQuery(rs.getString(QUERY.reference()));
        out.setMethod(rs.getString(METHOD.reference()));
        out.setStatus(rs.getInt(STATUS.reference()));
        out.setStart(fromNullableTimestamp(rs.getTimestamp(START.reference())));
        out.setEnd(fromNullableTimestamp(rs.getTimestamp(END.reference())));
        out.setThreadName(rs.getString(THREAD.reference()));
        out.setException(getExceptionInfoIfNotNull(rs.getString(ERR_TYPE.reference()), rs.getString(ERR_MSG.reference())));
        return out;
    }

    public static RestRequest restRequestMapperComplete(ResultSet rs) throws SQLException {
        if (rs.next()) {
           var out = new RestRequest();
            out.setCdSession(rs.getString(PARENT.reference()));
            out.setIdRequest(rs.getString(ID.reference()));
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
        }
        return null;
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

    public static RowMapper<Session> restSessionDumpMapper() {
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
            out.setHost(rs.getString(HOST.reference()));
            out.setThreadName(rs.getString(THREAD.reference()));
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
            setRestSessionMasks(out);
            return out;
        };
    }

    public static Session restSessionWithoutInstanceMapper(ResultSet rs) throws SQLException {
        if (rs.next()) {
            RestSession out = createBaseRestSession(rs);
            setRestSessionMasks(out);
            return out;
        }
        return null;
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

    private static void setRestSessionMasks(RestSession out) {
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

    private static void setMainSessionMasks(MainSession out) {
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

    public static RowMapper<Session> mainSessionWithInstanceMapper() {
        return rs -> {
            MainSession out = createBaseMainsession(rs);
            out.setAppName(rs.getString(APP_NAME.reference()));
            out.setOs(rs.getString(OS.reference()));
            out.setRe(rs.getString(RE.reference()));
            out.setAddress(rs.getString(ADDRESS.reference()));
            setMainSessionMasks(out);
            return out;
        };
    }

    public static Session mainSessionWithoutInstanceMapper(ResultSet rs) throws SQLException {
        if(rs.next()) {
            MainSession out = createBaseMainsession(rs);
            setMainSessionMasks(out);
            return out;
        }
        return null;
    }

    private static MainSession createBaseMainsession(ResultSet rs) throws SQLException {
        MainSession out = new MainSession();
        out.setId(rs.getString(ID.reference()));
        out.setName(rs.getString(NAME.reference()));
        out.setStart(fromNullableTimestamp(rs.getTimestamp(START.reference())));
        out.setEnd(fromNullableTimestamp(rs.getTimestamp(END.reference())));
        out.setType(rs.getString(TYPE.reference()));
        out.setLocation(rs.getString(LOCATION.reference()));
        out.setThreadName(rs.getString(THREAD.reference()));
        out.setException(getExceptionInfoIfNotNull(rs.getString(ERR_TYPE.reference()), rs.getString(ERR_MSG.reference())));
        out.setUser(rs.getString(USER.reference()));
        out.setInstanceId(rs.getString(INSTANCE_ENV.reference()));
        out.setMask(rs.getInt(MASK.reference()));
        return out;
    }


    
    public static RowMapper<Session> mainSessionDumpMapper(){
        return rs -> {
            MainSession out = new MainSession();
            out.setId(rs.getString(ID.reference())); // add value of nullable
            out.setName(rs.getString(NAME.reference()));
            out.setStart(fromNullableTimestamp(rs.getTimestamp(START.reference())));
            out.setEnd(fromNullableTimestamp(rs.getTimestamp(END.reference())));
            out.setType(rs.getString(TYPE.reference()));
            out.setLocation(rs.getString(LOCATION.reference()));
            out.setThreadName(rs.getString(THREAD.reference()));
            out.setException(getExceptionInfoIfNotNull(rs.getString(ERR_TYPE.reference()), rs.getString(ERR_MSG.reference())));
            return out;
        };
    }
    
    public static RowMapper<Session> mainSessionForSearchMapper(){
        return rs -> {
            MainSession out = new MainSession();
            out.setId(rs.getString(ID.reference())); // add value of nullable
            out.setName(rs.getString(NAME.reference()));
            out.setStart(fromNullableTimestamp(rs.getTimestamp(START.reference())));
            out.setEnd(fromNullableTimestamp(rs.getTimestamp(END.reference())));
            out.setLocation(rs.getString(LOCATION.reference()));
            out.setAppName(rs.getString(APP_NAME.reference()));
            out.setUser(rs.getString(USER.reference()));
            out.setType(rs.getString(TYPE.reference()));
            out.setException(getExceptionInfoIfNotNull(rs.getString(ERR_TYPE.reference()), rs.getString(ERR_MSG.reference())));
            return out;
        };
    }

    public static RowMapper<LocalRequest> localRequestMapper(){
        return rs -> {
            LocalRequest out = new LocalRequest();
            out.setCdSession(rs.getString(PARENT.reference()));
            out.setIdRequest(rs.getString(ID.reference()));
            out.setName(rs.getString(NAME.reference()));
            out.setLocation(rs.getString(LOCATION.reference()));
            out.setStart(fromNullableTimestamp(rs.getTimestamp(START.reference())));
            out.setEnd(fromNullableTimestamp(rs.getTimestamp(END.reference())));
            out.setUser(rs.getString(USER.reference()));
            out.setThreadName(rs.getString(THREAD.reference()));
            out.setStatus(rs.getBoolean(STATUS.reference()));
            out.setType(rs.getString(TYPE.reference()));
            out.setException(getExceptionInfoIfNotNull(rs.getString(ERR_TYPE.reference()), rs.getString(ERR_MSG.reference())));
            return out;
        };
    }

    public static RowMapper<DatabaseRequest> databaseRequestLazyMapper() {
        return InspectMappers::createBaseDatabaseRequest;
    }
    public static DatabaseRequest databaseRequestComplete(ResultSet rs) throws SQLException { // return null
        if (rs.next()) {
            DatabaseRequest out = createBaseDatabaseRequest(rs);
            out.setDriverVersion(rs.getString(DRIVER.reference()));
            out.setProductName(rs.getString(DB_NAME.reference()));
            out.setProductVersion(rs.getString(DB_VERSION.reference()));
            out.setPort(rs.getInt(PORT.reference()));
            out.setCdSession(rs.getString(PARENT.reference()));
            return out;
        }
        return null;
    }



    private static DatabaseRequest createBaseDatabaseRequest(ResultSet rs) throws SQLException {
        DatabaseRequest out = new DatabaseRequest();
        out.setIdRequest(rs.getString(ID.reference()));
        out.setHost(rs.getString(HOST.reference()));
        out.setName(rs.getString(DB.reference()));
        out.setStart(fromNullableTimestamp(rs.getTimestamp(START.reference())));
        out.setEnd(fromNullableTimestamp(rs.getTimestamp(END.reference())));
        out.setUser(rs.getString(USER.reference()));
        out.setThreadName(rs.getString(THREAD.reference()));
        out.setActions(new ArrayList<>());
        out.setCommand(rs.getString(COMMAND.reference()));
        out.setStatus(rs.getBoolean(STATUS.reference()));
        out.setSchema(rs.getString(SCHEMA.reference()));
        return out;
    }
    
    public static RowMapper<DatabaseRequestStage> databaseRequestStageMapper(){
        return rs -> {
            DatabaseRequestStage out= new DatabaseRequestStage();
            out.setName(rs.getString(NAME.reference()));
            out.setStart(fromNullableTimestamp(rs.getTimestamp(START.reference())));
            out.setEnd(fromNullableTimestamp(rs.getTimestamp(END.reference())));
            out.setCount(ofNullable(rs.getString(ACTION_COUNT.reference())).map(str -> Arrays.stream(str.split(",")).mapToLong(Long::parseLong).toArray()).orElse(null));
            out.setCommands(valueOfNullabletoEnumList(SqlCommand.class, rs.getString(COMMANDS.reference())).toArray(new SqlCommand[0]));
            out.setException(getExceptionInfoIfNotNull(rs.getString(ERR_TYPE.reference()), rs.getString(ERR_MSG.reference())));
            return out;
        };
    }

    public static RowMapper<FtpRequest> ftpRequestLazyMapper(){
        return InspectMappers::createBaseFtpRequest;
    }

    private static FtpRequest createBaseFtpRequest(ResultSet rs) throws SQLException {
        FtpRequest out = new FtpRequest();
        out.setIdRequest(rs.getString(ID.reference()));
        out.setHost(rs.getString(HOST.reference()));
        out.setStart(fromNullableTimestamp(rs.getTimestamp(START.reference())));
        out.setEnd(fromNullableTimestamp(rs.getTimestamp(END.reference())));
        out.setThreadName(rs.getString(THREAD.reference()));
        out.setActions(new ArrayList<>());
        out.setStatus(rs.getBoolean(STATUS.reference()));
        return out;
    }

    public static FtpRequest ftpRequestComplete(ResultSet rs) throws SQLException {
        if (rs.next()) {
            FtpRequest out = createBaseFtpRequest(rs);
            out.setCdSession(rs.getString(PARENT.reference()));
            out.setPort(rs.getInt(PORT.reference()));
            out.setProtocol(rs.getString(PROTOCOL.reference()));
            out.setServerVersion(rs.getString(SERVER_VERSION.reference()));
            out.setClientVersion(rs.getString(CLIENT_VERSION.reference()));
            out.setUser(rs.getString(USER.reference()));
            return out;
        }
        return null;
    }
    
    public static RowMapper<FtpRequestStage> ftpRequestStageMapper(){
        return rs -> {
            FtpRequestStage out = new FtpRequestStage();
            out.setName(rs.getString(NAME.reference()));
            out.setStart(fromNullableTimestamp(rs.getTimestamp(START.reference())));
            out.setEnd(fromNullableTimestamp(rs.getTimestamp(END.reference())));
            out.setArgs(ofNullable(rs.getString(ARG.reference())).map(str -> Arrays.stream(str.split(",")).toArray(String[]::new)).orElse(null));
            out.setException(getExceptionInfoIfNotNull(rs.getString(ERR_TYPE.reference()), rs.getString(ERR_MSG.reference())));
            return out;
        };
    }

    public static RowMapper<MailRequest> smtpRequestLazyMapper(){
        return InspectMappers::createBaseMailRequest;
    }

    private static MailRequest createBaseMailRequest(ResultSet rs) throws SQLException{
        MailRequest out = new MailRequest();
        out.setIdRequest(rs.getString(ID.reference()));
        out.setHost(rs.getString(HOST.reference()));
        out.setStart(fromNullableTimestamp(rs.getTimestamp(START.reference())));
        out.setEnd(fromNullableTimestamp(rs.getTimestamp(END.reference())));
        out.setThreadName(rs.getString(THREAD.reference()));
        out.setActions(new ArrayList<>());
        out.setStatus(rs.getBoolean(STATUS.reference()));
        return out;
    }

    public static MailRequest mailRequestCompleteMapper(ResultSet rs) throws SQLException {
        if (rs.next()) {
            MailRequest out = createBaseMailRequest(rs);
            out.setCdSession(rs.getString(PARENT.reference()));
            out.setPort(rs.getInt(PORT.reference()));
            out.setUser(rs.getString(USER.reference()));
            return out;
        }
        return null;
    }

    public static RowMapper<MailRequestStage> mailRequestStageMapper(){
        return rs -> {
            MailRequestStage out = new MailRequestStage();
            out.setName(rs.getString(NAME.reference()));
            out.setStart(fromNullableTimestamp(rs.getTimestamp(START.reference())));
            out.setEnd(fromNullableTimestamp(rs.getTimestamp(END.reference())));
            out.setException(getExceptionInfoIfNotNull(rs.getString(ERR_TYPE.reference()), rs.getString(ERR_MSG.reference())));
            return out;
        };
    }
    
    public static RowMapper<Mail> mailMapper(){
        return rs -> {
            var out = new Mail();
            out.setContentType(rs.getString(MEDIA.reference()));
            out.setFrom(ofNullable(rs.getString(FROM.reference())).map(str -> Arrays.stream(str.split(",")).toArray(String[]::new)).orElse(null));
            out.setRecipients(ofNullable(rs.getString(RECIPIENTS.reference())).map(str -> Arrays.stream(str.split(",")).toArray(String[]::new)).orElse(null));
            out.setReplyTo(ofNullable(rs.getString(REPLY_TO.reference())).map(str -> Arrays.stream(str.split(",")).toArray(String[]::new)).orElse(null));
            out.setSize(rs.getInt(SIZE.reference()));
            out.setSubject(rs.getString(SUBJECT.reference()));
            return out;
        };
    }

    public static RowMapper<NamingRequest> ldapRequestLazyMapper(){
        return InspectMappers::createBaseLdapRequest;
    }

    private static NamingRequest createBaseLdapRequest(ResultSet rs) throws SQLException{
        NamingRequest out = new NamingRequest();
        out.setIdRequest(rs.getString(ID.reference()));
        out.setHost(rs.getString(HOST.reference()));
        out.setStart(fromNullableTimestamp(rs.getTimestamp(START.reference())));
        out.setEnd(fromNullableTimestamp(rs.getTimestamp(END.reference())));
        out.setThreadName(rs.getString(THREAD.reference()));
        out.setActions(new ArrayList<>());
        out.setStatus(rs.getBoolean(STATUS.reference()));
        return out;
    }

    public static NamingRequest ldapRequestCompleteMapper(ResultSet rs) throws SQLException {
        if (rs.next()) {
            NamingRequest out = createBaseLdapRequest(rs);
            out.setCdSession(rs.getString(PARENT.reference()));
            out.setPort(rs.getInt(PORT.reference()));
            out.setProtocol(rs.getString(PROTOCOL.reference()));
            out.setUser(rs.getString(USER.reference()));
            return out;
        }
        return null;
    }
    
    public static RowMapper<NamingRequestStage> ldapRequestStageMapper(){
        return rs -> {
            var out = new NamingRequestStage();
            out.setName(rs.getString(NAME.reference()));
            out.setStart(fromNullableTimestamp(rs.getTimestamp(START.reference())));
            out.setEnd(fromNullableTimestamp(rs.getTimestamp(END.reference())));
            out.setException(getExceptionInfoIfNotNull(rs.getString(ERR_TYPE.reference()), rs.getString(ERR_MSG.reference())));
            return out;
        };
    }

    public static RowMapper<UserAction> userActionMapper(){
        return rs -> {
                UserAction out = new UserAction(
                    rs.getString(NAME.reference()),
                    rs.getString(NODE_NAME.reference()),
                    rs.getString(TYPE.reference()),
                    fromNullableTimestamp(rs.getTimestamp(START.reference()))
            );
            out.setCdSession(rs.getString(PARENT.reference()));
            return out;
        };

    }



    public static Map<Long, ExceptionInfo> exceptionInfoMapper(ResultSet rs) throws SQLException {
        Map<Long, ExceptionInfo> out = new HashMap<>();
        while(rs.next()) {
            out.put(rs.getLong(PARENT.reference()), new ExceptionInfo(rs.getString(ERR_TYPE.reference()), rs.getString(ERR_MSG.reference())));
        }
        return out;
    }

}
