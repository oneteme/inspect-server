package org.usf.inspect.server.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.usf.inspect.core.*;
import org.usf.inspect.jdbc.SqlCommand;
import org.usf.inspect.server.RequestMask;
import org.usf.inspect.server.model.InstanceTrace;
import org.usf.inspect.server.model.Session;
import org.usf.inspect.server.model.UserAction;
import org.usf.inspect.server.model.wrapper.*;
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
import static org.usf.inspect.server.service.RequestService.valueOfNullabletoEnumList;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class InspectMappers {

    public static InstanceEnvironment instanceEnvironmentMapper(ResultSet rs) throws SQLException {
        var mapper = new ObjectMapper();
        if(rs.next()) {
            InstanceEnvironment instanceEnvironment;
            try {
                instanceEnvironment = new InstanceEnvironment(
                        rs.getString(ID.reference()),
                        fromNullableTimestamp(rs.getTimestamp(START.reference())),
                        InstanceType.valueOf(rs.getString(TYPE.reference())),
                        rs.getString(APP_NAME.reference()),
                        rs.getString(VERSION.reference()),
                        rs.getString(ENVIRONEMENT.reference()),
                        rs.getString(ADDRESS.reference()),
                        rs.getString(OS.reference()),
                        rs.getString(RE.reference()),
                        rs.getString(USER.reference()),
                        rs.getString(BRANCH.reference()),
                        rs.getString(HASH.reference()),
                        rs.getString(COLLECTOR.reference()),
                        rs.getString(ADDITIONAL_PROPERTIES.reference()) != null ? mapper.readValue(rs.getString(ADDITIONAL_PROPERTIES.reference()), new TypeReference<Map<String, String>>() {}) : null,
                        rs.getString(CONFIGURATION.reference()) != null ? mapper.readValue(rs.getString(CONFIGURATION.reference()), InspectCollectorConfiguration.class) : null
                );
                instanceEnvironment.setResource(rs.getString(RESOURCE.reference()) != null ? mapper.readValue(rs.getString(RESOURCE.reference()), MachineResource.class) : null);
                instanceEnvironment.setEnd(fromNullableTimestamp(rs.getTimestamp(END.reference())));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
            return instanceEnvironment;
        }
        return null;
    }

    public static RowMapper<InstanceTrace> instanceTraceMapper() {
        return rs ->
            new InstanceTrace(
                    rs.getInt(PENDING.reference()),
                    rs.getInt(ATTEMPTS.reference()),
                    rs.getInt(SIZE_SESSION.reference()),
                    rs.getString(FILENAME.reference()),
                    fromNullableTimestamp(rs.getTimestamp(START.reference())),
                    rs.getString(INSTANCE_ENV.reference())
            );
    }

    public static RowMapper<MachineResourceUsage> instanceResourceUsageMapper() {
        return rs ->
                new MachineResourceUsage(
                        fromNullableTimestamp(rs.getTimestamp(START.reference())),
                        rs.getInt(USED_HEAP.reference()),
                        rs.getInt(COMMITED_HEAP.reference()),
                        rs.getInt(USED_META.reference()),
                        rs.getInt(COMMITED_META.reference()),
                        rs.getInt(USED_DISK_SPACE.reference())
                );
    }

    public static RowMapper<LogEntry> instanceLogEntryMapper() {
        var mapper = new ObjectMapper();
        return rs -> {
            try {
                var log = new LogEntry(
                        fromNullableTimestamp(rs.getTimestamp(START.reference())),
                        LogEntry.Level.valueOf(rs.getString(LOG_LEVEL.reference())),
                        rs.getString(LOG_MESSAGE.reference()),
                        rs.getString(STACKTRACE.reference()) != null ? mapper.readValue(rs.getString(STACKTRACE.reference()), new TypeReference<StackTraceRow[]>() {}) : null
                );
                log.setSessionId(rs.getString(PARENT.reference()));
                log.setInstanceId(rs.getString(INSTANCE_ENV.reference()));
                return log;
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        };
    }

    public static RowMapper<RestRequest> restRequestLazyMapper() {
       return InspectMappers::createBaseRestRequest;
    }

    public static RestRequest createBaseRestRequest(ResultSet rs) throws SQLException {
        RestRequest out = new RestRequest();
        out.setId(rs.getString(ID.reference()));
        out.setProtocol(rs.getString(PROTOCOL.reference()));
        out.setHost(rs.getString(HOST.reference()));
        out.setPath(rs.getString(PATH.reference()));
        out.setQuery(rs.getString(QUERY.reference()));
        out.setMethod(rs.getString(METHOD.reference()));
        out.setStatus(rs.getInt(STATUS.reference()));
        out.setStart(fromNullableTimestamp(rs.getTimestamp(START.reference())));
        out.setEnd(fromNullableTimestamp(rs.getTimestamp(END.reference())));
        out.setThreadName(rs.getString(THREAD.reference()));
        //out.setException(getExceptionInfoIfNotNull(rs.getString(ERR_TYPE.reference()), rs.getString(ERR_MSG.reference())));
        return out;
    }

    public static RestRequest restRequestMapperComplete(ResultSet rs) throws SQLException {
        if (rs.next()) {
           var out = new RestRequest();
            out.setSessionId(rs.getString(PARENT.reference()));
            out.setId(rs.getString(ID.reference()));
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
            //out.setException(getExceptionInfoIfNotNull(rs.getString(ERR_TYPE.reference()), rs.getString(ERR_MSG.reference())));
            return out;
        }
        return null;
    }
    
    public static RowMapper<RestSessionWrapper> restSessionShallowMapper() {
        return rs -> {
            RestSessionWrapper out = new RestSessionWrapper();
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
            //out.setException(getExceptionInfoIfNotNull(rs.getString(ERR_TYPE.reference()), rs.getString(ERR_MSG.reference())));
            return out;
        };
    }

    public static RowMapper<Session> restSessionDumpMapper() {
        return rs -> {
            RestSessionWrapper out = new RestSessionWrapper();
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
            //out.setException(getExceptionInfoIfNotNull(rs.getString(ERR_TYPE.reference()), rs.getString(ERR_MSG.reference())));
            return out;
        };
    }

    public static RowMapper<Session> restSessionWithInstanceMapper() {
        return rs -> {
            RestSessionWrapper out = createBaseRestSession(rs);
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
            RestSessionWrapper out = createBaseRestSession(rs);
            setRestSessionMasks(out);
            return out;
        }
        return null;
    }


    private static RestSessionWrapper createBaseRestSession(ResultSet rs) throws SQLException {
        RestSessionWrapper out = new RestSessionWrapper();
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
        //out.setException(getExceptionInfoIfNotNull(rs.getString(ERR_TYPE.reference()), rs.getString(ERR_MSG.reference())));
        out.setName(rs.getString(API_NAME.reference()));
        out.setUserAgent(rs.getString(USER_AGT.reference()));
        out.setUser(rs.getString(USER.reference()));
        out.setMask(rs.getInt(MASK.reference()));
        out.setInstanceId(rs.getString(INSTANCE_ENV.reference()));
        out.setCacheControl(rs.getString(CACHE_CONTROL.reference()));
        return out;
    }

    private static void setRestSessionMasks(RestSessionWrapper out) {
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

    private static void setMainSessionMasks(MainSessionWrapper out) {
        if (RequestMask.JDBC.is(out.getRequestsMask())) {
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
            MainSessionWrapper out = createBaseMainsession(rs);
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
            MainSessionWrapper out = createBaseMainsession(rs);
            setMainSessionMasks(out);
            return out;
        }
        return null;
    }

    private static MainSessionWrapper createBaseMainsession(ResultSet rs) throws SQLException {
        MainSessionWrapper out = new MainSessionWrapper();
        out.setId(rs.getString(ID.reference()));
        out.setName(rs.getString(NAME.reference()));
        out.setStart(fromNullableTimestamp(rs.getTimestamp(START.reference())));
        out.setEnd(fromNullableTimestamp(rs.getTimestamp(END.reference())));
        out.setType(rs.getString(TYPE.reference()));
        out.setLocation(rs.getString(LOCATION.reference()));
        out.setThreadName(rs.getString(THREAD.reference()));
        //out.setException(getExceptionInfoIfNotNull(rs.getString(ERR_TYPE.reference()), rs.getString(ERR_MSG.reference())));
        out.setUser(rs.getString(USER.reference()));
        out.setInstanceId(rs.getString(INSTANCE_ENV.reference()));
        out.setMask(rs.getInt(MASK.reference()));
        return out;
    }


    
    public static RowMapper<Session> mainSessionDumpMapper(){
        return rs -> {
            MainSessionWrapper out = new MainSessionWrapper();
            out.setId(rs.getString(ID.reference())); // add value of nullable
            out.setName(rs.getString(NAME.reference()));
            out.setStart(fromNullableTimestamp(rs.getTimestamp(START.reference())));
            out.setEnd(fromNullableTimestamp(rs.getTimestamp(END.reference())));
            out.setType(rs.getString(TYPE.reference()));
            out.setLocation(rs.getString(LOCATION.reference()));
            out.setThreadName(rs.getString(THREAD.reference()));
            //out.setException(getExceptionInfoIfNotNull(rs.getString(ERR_TYPE.reference()), rs.getString(ERR_MSG.reference())));
            return out;
        };
    }
    
    public static RowMapper<Session> mainSessionForSearchMapper(){
        return rs -> {
            MainSessionWrapper out = new MainSessionWrapper();
            out.setId(rs.getString(ID.reference())); // add value of nullable
            out.setName(rs.getString(NAME.reference()));
            out.setStart(fromNullableTimestamp(rs.getTimestamp(START.reference())));
            out.setEnd(fromNullableTimestamp(rs.getTimestamp(END.reference())));
            out.setLocation(rs.getString(LOCATION.reference()));
            out.setAppName(rs.getString(APP_NAME.reference()));
            out.setUser(rs.getString(USER.reference()));
            out.setType(rs.getString(TYPE.reference()));
            //out.setException(getExceptionInfoIfNotNull(rs.getString(ERR_TYPE.reference()), rs.getString(ERR_MSG.reference())));
            return out;
        };
    }

    public static RowMapper<LocalRequestWrapper> localRequestMapper(){
        return rs -> {
            LocalRequestWrapper out = new LocalRequestWrapper();
            out.setSessionId(rs.getString(PARENT.reference()));
            out.setId(rs.getString(ID.reference()));
            out.setName(rs.getString(NAME.reference()));
            out.setLocation(rs.getString(LOCATION.reference()));
            out.setStart(fromNullableTimestamp(rs.getTimestamp(START.reference())));
            out.setEnd(fromNullableTimestamp(rs.getTimestamp(END.reference())));
            out.setUser(rs.getString(USER.reference()));
            out.setThreadName(rs.getString(THREAD.reference()));
            out.setFailed(rs.getBoolean(FAILED.reference()));
            out.setType(rs.getString(TYPE.reference()));
            //out.setException(getExceptionInfoIfNotNull(rs.getString(ERR_TYPE.reference()), rs.getString(ERR_MSG.reference())));
            return out;
        };
    }

    public static RowMapper<DatabaseRequestWrapper> databaseRequestLazyMapper() {
        return InspectMappers::createBaseDatabaseRequest;
    }
    public static DatabaseRequestWrapper databaseRequestComplete(ResultSet rs) throws SQLException { // return null
        if (rs.next()) {
            DatabaseRequestWrapper out = createBaseDatabaseRequest(rs);
            out.setDriverVersion(rs.getString(DRIVER.reference()));
            out.setProductName(rs.getString(DB_NAME.reference()));
            out.setProductVersion(rs.getString(DB_VERSION.reference()));
            out.setPort(rs.getInt(PORT.reference()));
            out.setSessionId(rs.getString(PARENT.reference()));
            return out;
        }
        return null;
    }



    private static DatabaseRequestWrapper createBaseDatabaseRequest(ResultSet rs) throws SQLException {
        DatabaseRequestWrapper out = new DatabaseRequestWrapper();
        out.setId(rs.getString(ID.reference()));
        out.setHost(rs.getString(HOST.reference()));
        out.setName(rs.getString(DB.reference()));
        out.setStart(fromNullableTimestamp(rs.getTimestamp(START.reference())));
        out.setEnd(fromNullableTimestamp(rs.getTimestamp(END.reference())));
        out.setUser(rs.getString(USER.reference()));
        out.setThreadName(rs.getString(THREAD.reference()));
        out.setActions(new ArrayList<>());
        //out.setCommand(rs.getString(COMMAND.reference()));
        out.setFailed(rs.getBoolean(FAILED.reference()));
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
            //out.setException(getExceptionInfoIfNotNull(rs.getString(ERR_TYPE.reference()), rs.getString(ERR_MSG.reference())));
            return out;
        };
    }

    public static RowMapper<FtpRequestWrapper> ftpRequestLazyMapper(){
        return InspectMappers::createBaseFtpRequest;
    }

    private static FtpRequestWrapper createBaseFtpRequest(ResultSet rs) throws SQLException {
        FtpRequestWrapper out = new FtpRequestWrapper();
        out.setId(rs.getString(ID.reference()));
        out.setHost(rs.getString(HOST.reference()));
        out.setStart(fromNullableTimestamp(rs.getTimestamp(START.reference())));
        out.setEnd(fromNullableTimestamp(rs.getTimestamp(END.reference())));
        out.setThreadName(rs.getString(THREAD.reference()));
        out.setActions(new ArrayList<>());
        out.setFailed(rs.getBoolean(FAILED.reference()));
        return out;
    }

    public static FtpRequestWrapper ftpRequestComplete(ResultSet rs) throws SQLException {
        if (rs.next()) {
            FtpRequestWrapper out = createBaseFtpRequest(rs);
            out.setSessionId(rs.getString(PARENT.reference()));
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
            //out.setException(getExceptionInfoIfNotNull(rs.getString(ERR_TYPE.reference()), rs.getString(ERR_MSG.reference())));
            return out;
        };
    }

    public static RowMapper<MailRequestWrapper> smtpRequestLazyMapper(){
        return InspectMappers::createBaseMailRequest;
    }

    private static MailRequestWrapper createBaseMailRequest(ResultSet rs) throws SQLException{
        MailRequestWrapper out = new MailRequestWrapper();
        out.setId(rs.getString(ID.reference()));
        out.setHost(rs.getString(HOST.reference()));
        out.setStart(fromNullableTimestamp(rs.getTimestamp(START.reference())));
        out.setEnd(fromNullableTimestamp(rs.getTimestamp(END.reference())));
        out.setThreadName(rs.getString(THREAD.reference()));
        out.setActions(new ArrayList<>());
        out.setFailed(rs.getBoolean(FAILED.reference()));
        return out;
    }

    public static MailRequestWrapper mailRequestCompleteMapper(ResultSet rs) throws SQLException {
        if (rs.next()) {
            MailRequestWrapper out = createBaseMailRequest(rs);
            out.setSessionId(rs.getString(PARENT.reference()));
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
            //out.setException(getExceptionInfoIfNotNull(rs.getString(ERR_TYPE.reference()), rs.getString(ERR_MSG.reference())));
            return out;
        };
    }
    
    public static RowMapper<MailWrapper> mailMapper(){
        return rs -> {
            var out = new MailWrapper();
            out.setContentType(rs.getString(MEDIA.reference()));
            out.setFrom(ofNullable(rs.getString(FROM.reference())).map(str -> Arrays.stream(str.split(",")).toArray(String[]::new)).orElse(null));
            out.setRecipients(ofNullable(rs.getString(RECIPIENTS.reference())).map(str -> Arrays.stream(str.split(",")).toArray(String[]::new)).orElse(null));
            out.setReplyTo(ofNullable(rs.getString(REPLY_TO.reference())).map(str -> Arrays.stream(str.split(",")).toArray(String[]::new)).orElse(null));
            out.setSize(rs.getInt(SIZE.reference()));
            out.setSubject(rs.getString(SUBJECT.reference()));
            return out;
        };
    }

    public static RowMapper<DirectoryRequestWrapper> ldapRequestLazyMapper(){
        return InspectMappers::createBaseLdapRequest;
    }

    private static DirectoryRequestWrapper createBaseLdapRequest(ResultSet rs) throws SQLException{
        DirectoryRequestWrapper out = new DirectoryRequestWrapper();
        out.setId(rs.getString(ID.reference()));
        out.setHost(rs.getString(HOST.reference()));
        out.setStart(fromNullableTimestamp(rs.getTimestamp(START.reference())));
        out.setEnd(fromNullableTimestamp(rs.getTimestamp(END.reference())));
        out.setThreadName(rs.getString(THREAD.reference()));
        out.setActions(new ArrayList<>());
        out.setFailed(rs.getBoolean(FAILED.reference()));
        return out;
    }

    public static DirectoryRequestWrapper ldapRequestCompleteMapper(ResultSet rs) throws SQLException {
        if (rs.next()) {
            DirectoryRequestWrapper out = createBaseLdapRequest(rs);
            out.setSessionId(rs.getString(PARENT.reference()));
            out.setPort(rs.getInt(PORT.reference()));
            out.setProtocol(rs.getString(PROTOCOL.reference()));
            out.setUser(rs.getString(USER.reference()));
            return out;
        }
        return null;
    }
    
    public static RowMapper<DirectoryRequestStage> ldapRequestStageMapper(){
        return rs -> {
            var out = new DirectoryRequestStage();
            out.setName(rs.getString(NAME.reference()));
            out.setStart(fromNullableTimestamp(rs.getTimestamp(START.reference())));
            out.setEnd(fromNullableTimestamp(rs.getTimestamp(END.reference())));
            //out.setException(getExceptionInfoIfNotNull(rs.getString(ERR_TYPE.reference()), rs.getString(ERR_MSG.reference())));
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



    public static Map<Long, ExceptionInfoWrapper> exceptionInfoMapper(ResultSet rs) throws SQLException {
        Map<Long, ExceptionInfoWrapper> out = new HashMap<>();
        while(rs.next()) {
            //out.put(rs.getLong(PARENT.reference()), new ExceptionInfoWrapper(rs.getString(ERR_TYPE.reference()), rs.getString(ERR_MSG.reference())));
        }
        return out;
    }

}
