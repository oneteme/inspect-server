package org.usf.inspect.server.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.usf.inspect.core.*;
import org.usf.inspect.jdbc.SqlCommand;
import org.usf.inspect.server.dto.*;
import org.usf.inspect.server.model.InstanceTrace;
import org.usf.inspect.server.model.UserAction;
import org.usf.jquery.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
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
        if(rs.next()) {
            var instanceEnvironment = new InstanceEnvironment(
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
                    null,
                    null
                    //rs.getString(ADDITIONAL_PROPERTIES.reference()) != null ? mapper.readValue(rs.getString(ADDITIONAL_PROPERTIES.reference()), new TypeReference<Map<String, String>>() {}) : null,
                    //rs.getString(CONFIGURATION.reference()) != null ? mapper.readValue(rs.getString(CONFIGURATION.reference()), InspectCollectorConfiguration.class) : null
            );
            //instanceEnvironment.setResource(rs.getString(RESOURCE.reference()) != null ? mapper.readValue(rs.getString(RESOURCE.reference()), MachineResource.class) : null);
            instanceEnvironment.setEnd(fromNullableTimestamp(rs.getTimestamp(END.reference())));
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

    public static RowMapper<RestRequestDto> restRequestLazyMapper() {
        return rs -> {
            RestRequestDto out = createBaseRestRequest(rs);
            out.setException(getExceptionInfoIfNotNull(rs.getString(ERR_TYPE.reference()), rs.getString(ERR_MSG.reference()), null));
            return out;
        };
    }

    public static RestRequestDto createBaseRestRequest(ResultSet rs) throws SQLException {
        RestRequestDto out = new RestRequestDto();
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
        out.setBodyContent(rs.getString(BODY_CONTENT.reference()));
        return out;
    }

    public static RestRequest restRequestMapperComplete(ResultSet rs) throws SQLException {
        if (rs.next()) {
            var out = createBaseRestRequest(rs);
            out.setSessionId(rs.getString(PARENT.reference()));
            out.setPort(rs.getInt(PORT.reference()));
            out.setInDataSize(rs.getLong(SIZE_IN.reference()));
            out.setOutDataSize(rs.getLong(SIZE_OUT.reference()));
            out.setInContentEncoding(rs.getString(CONTENT_ENCODING_IN.reference()));
            out.setOutContentEncoding(rs.getString(CONTENT_ENCODING_OUT.reference()));
            out.setAuthScheme(rs.getString(AUTH.reference()));
            return out;
        }
        return null;
    }
    
    public static RowMapper<RestSessionDto> restSessionShallowMapper() {
        return rs -> {
            RestSessionDto out = new RestSessionDto();
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
            out.setException(getExceptionInfoIfNotNull(rs.getString(ERR_TYPE.reference()), rs.getString(ERR_MSG.reference()), null));
            return out;
        };
    }

    public static RowMapper<RestSession> restSessionDumpMapper() {
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
            out.setException(getExceptionInfoIfNotNull(rs.getString(ERR_TYPE.reference()), rs.getString(ERR_MSG.reference()), null));
            return out;
        };
    }

    public static RestSession createBaseRestSession(ResultSet rs) throws SQLException {
        if (rs.next()) {
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
            out.setException(getExceptionInfoIfNotNull(rs.getString(ERR_TYPE.reference()), rs.getString(ERR_MSG.reference()), null));
            out.setName(rs.getString(API_NAME.reference()));
            out.setUserAgent(rs.getString(USER_AGT.reference()));
            out.setUser(rs.getString(USER.reference()));
            out.setRequestsMask(rs.getInt(MASK.reference()));
            out.setInstanceId(rs.getString(INSTANCE_ENV.reference()));
            out.setCacheControl(rs.getString(CACHE_CONTROL.reference()));
            return out;
        }
        return null;
    }

    public static MainSession createBaseMainsession(ResultSet rs) throws SQLException {
        if(rs.next()) {
            MainSession out = new MainSession();
            out.setId(rs.getString(ID.reference()));
            out.setName(rs.getString(NAME.reference()));
            out.setStart(fromNullableTimestamp(rs.getTimestamp(START.reference())));
            out.setEnd(fromNullableTimestamp(rs.getTimestamp(END.reference())));
            out.setType(rs.getString(TYPE.reference()));
            out.setLocation(rs.getString(LOCATION.reference()));
            out.setThreadName(rs.getString(THREAD.reference()));
            out.setException(getExceptionInfoIfNotNull(rs.getString(ERR_TYPE.reference()), rs.getString(ERR_MSG.reference()), null));
            out.setUser(rs.getString(USER.reference()));
            out.setInstanceId(rs.getString(INSTANCE_ENV.reference()));
            out.setRequestsMask(rs.getInt(MASK.reference()));
            return out;
        }
        return null;
    }


    
    public static RowMapper<MainSession> mainSessionDumpMapper(){
        return rs -> {
            MainSession out = new MainSession();
            out.setId(rs.getString(ID.reference())); // add value of nullable
            out.setName(rs.getString(NAME.reference()));
            out.setStart(fromNullableTimestamp(rs.getTimestamp(START.reference())));
            out.setEnd(fromNullableTimestamp(rs.getTimestamp(END.reference())));
            out.setType(rs.getString(TYPE.reference()));
            out.setLocation(rs.getString(LOCATION.reference()));
            out.setThreadName(rs.getString(THREAD.reference()));
            out.setException(getExceptionInfoIfNotNull(rs.getString(ERR_TYPE.reference()), rs.getString(ERR_MSG.reference()), null));
            return out;
        };
    }
    
    public static RowMapper<MainSessionDto> mainSessionForSearchMapper(){
        return rs -> {
            MainSessionDto out = new MainSessionDto();
            out.setAppName(rs.getString(APP_NAME.reference()));
            out.setId(rs.getString(ID.reference())); // add value of nullable
            out.setName(rs.getString(NAME.reference()));
            out.setStart(fromNullableTimestamp(rs.getTimestamp(START.reference())));
            out.setEnd(fromNullableTimestamp(rs.getTimestamp(END.reference())));
            out.setLocation(rs.getString(LOCATION.reference()));
            out.setUser(rs.getString(USER.reference()));
            out.setType(rs.getString(TYPE.reference()));
            out.setException(getExceptionInfoIfNotNull(rs.getString(ERR_TYPE.reference()), rs.getString(ERR_MSG.reference()), null));
            return out;
        };
    }

    public static RowMapper<LocalRequest> localRequestMapper(){
        return rs -> {
            LocalRequest out = new LocalRequest();
            out.setId(rs.getString(ID.reference()));
            out.setName(rs.getString(NAME.reference()));
            out.setLocation(rs.getString(LOCATION.reference()));
            out.setStart(fromNullableTimestamp(rs.getTimestamp(START.reference())));
            out.setEnd(fromNullableTimestamp(rs.getTimestamp(END.reference())));
            out.setUser(rs.getString(USER.reference()));
            out.setThreadName(rs.getString(THREAD.reference()));
            out.setType(rs.getString(TYPE.reference()));
            out.setException(getExceptionInfoIfNotNull(rs.getString(ERR_TYPE.reference()), rs.getString(ERR_MSG.reference()), null));
            return out;
        };
    }

    public static RowMapper<DatabaseRequestDto> databaseRequestLazyMapper() {
        return rs -> {
            DatabaseRequestDto out = createBaseDatabaseRequest(rs);
            out.setException(getExceptionInfoIfNotNull(rs.getString(ERR_TYPE.reference()), rs.getString(ERR_MSG.reference()), null));
            return out;
        };
    }

    public static DatabaseRequest databaseRequestComplete(ResultSet rs) throws SQLException { // return null
        if (rs.next()) {
            DatabaseRequest out = createBaseDatabaseRequest(rs);
            out.setDriverVersion(rs.getString(DRIVER.reference()));
            out.setProductName(rs.getString(DB_NAME.reference()));
            out.setProductVersion(rs.getString(DB_VERSION.reference()));
            out.setPort(rs.getInt(PORT.reference()));
            out.setSessionId(rs.getString(PARENT.reference()));
            return out;
        }
        return null;
    }



    private static DatabaseRequestDto createBaseDatabaseRequest(ResultSet rs) throws SQLException {
        DatabaseRequestDto out = new DatabaseRequestDto();
        out.setId(rs.getString(ID.reference()));
        out.setHost(rs.getString(HOST.reference()));
        out.setName(rs.getString(DB.reference()));
        out.setStart(fromNullableTimestamp(rs.getTimestamp(START.reference())));
        out.setEnd(fromNullableTimestamp(rs.getTimestamp(END.reference())));
        out.setUser(rs.getString(USER.reference()));
        out.setThreadName(rs.getString(THREAD.reference()));
        out.setCommand(rs.getString(COMMAND.reference()));
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
            out.setException(getExceptionInfoIfNotNull(rs.getString(ERR_TYPE.reference()), rs.getString(ERR_MSG.reference()), null));
            out.setOrder(rs.getInt(ORDER.reference()));
            return out;
        };
    }

    public static RowMapper<HttpRequestStage> restRequestStageMapper(){
        return rs -> {
            HttpRequestStage out= new HttpRequestStage();
            out.setName(rs.getString(NAME.reference()));
            out.setStart(fromNullableTimestamp(rs.getTimestamp(START.reference())));
            out.setEnd(fromNullableTimestamp(rs.getTimestamp(END.reference())));
            out.setException(getExceptionInfoIfNotNull(rs.getString(ERR_TYPE.reference()), rs.getString(ERR_MSG.reference()), null));
            out.setOrder(rs.getInt(ORDER.reference()));
            return out;
        };
    }

    public static RowMapper<FtpRequestDto> ftpRequestLazyMapper(){
        return rs -> {
            FtpRequestDto out = createBaseFtpRequest(rs);
            out.setException(getExceptionInfoIfNotNull(rs.getString(ERR_TYPE.reference()), rs.getString(ERR_MSG.reference()), null));
            return out;
        };
    }

    private static FtpRequestDto createBaseFtpRequest(ResultSet rs) throws SQLException {
        FtpRequestDto out = new FtpRequestDto();
        out.setId(rs.getString(ID.reference()));
        out.setHost(rs.getString(HOST.reference()));
        out.setStart(fromNullableTimestamp(rs.getTimestamp(START.reference())));
        out.setEnd(fromNullableTimestamp(rs.getTimestamp(END.reference())));
        out.setThreadName(rs.getString(THREAD.reference()));
        return out;
    }

    public static FtpRequest ftpRequestComplete(ResultSet rs) throws SQLException {
        if (rs.next()) {
            FtpRequest out = createBaseFtpRequest(rs);
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
            out.setException(getExceptionInfoIfNotNull(rs.getString(ERR_TYPE.reference()), rs.getString(ERR_MSG.reference()), null));
            out.setOrder(rs.getInt(ORDER.reference()));
            return out;
        };
    }

    public static RowMapper<MailRequestDto> smtpRequestLazyMapper(){
        return rs -> {
            MailRequestDto out = createBaseMailRequest(rs);
            out.setException(getExceptionInfoIfNotNull(rs.getString(ERR_TYPE.reference()), rs.getString(ERR_MSG.reference()), null));
            return out;
        };
    }

    private static MailRequestDto createBaseMailRequest(ResultSet rs) throws SQLException{
        MailRequestDto out = new MailRequestDto();
        out.setId(rs.getString(ID.reference()));
        out.setHost(rs.getString(HOST.reference()));
        out.setStart(fromNullableTimestamp(rs.getTimestamp(START.reference())));
        out.setEnd(fromNullableTimestamp(rs.getTimestamp(END.reference())));
        out.setThreadName(rs.getString(THREAD.reference()));
        return out;
    }

    public static MailRequest mailRequestCompleteMapper(ResultSet rs) throws SQLException {
        if (rs.next()) {
            MailRequest out = createBaseMailRequest(rs);
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
            out.setException(getExceptionInfoIfNotNull(rs.getString(ERR_TYPE.reference()), rs.getString(ERR_MSG.reference()), null));
            out.setOrder(rs.getInt(ORDER.reference()));
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

    public static RowMapper<DirectoryRequestDto> ldapRequestLazyMapper(){
        return rs -> {
            DirectoryRequestDto out = createBaseLdapRequest(rs);
            out.setException(getExceptionInfoIfNotNull(rs.getString(ERR_TYPE.reference()), rs.getString(ERR_MSG.reference()), null));
            return out;
        };
    }

    private static DirectoryRequestDto createBaseLdapRequest(ResultSet rs) throws SQLException{
        DirectoryRequestDto out = new DirectoryRequestDto();
        out.setId(rs.getString(ID.reference()));
        out.setHost(rs.getString(HOST.reference()));
        out.setStart(fromNullableTimestamp(rs.getTimestamp(START.reference())));
        out.setEnd(fromNullableTimestamp(rs.getTimestamp(END.reference())));
        out.setThreadName(rs.getString(THREAD.reference()));
        return out;
    }

    public static DirectoryRequest ldapRequestCompleteMapper(ResultSet rs) throws SQLException {
        if (rs.next()) {
            DirectoryRequest out = createBaseLdapRequest(rs);
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
            out.setException(getExceptionInfoIfNotNull(rs.getString(ERR_TYPE.reference()), rs.getString(ERR_MSG.reference()), null));
            out.setOrder(rs.getInt(ORDER.reference()));
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
            out.put(rs.getLong(PARENT.reference()), new ExceptionInfo(rs.getString(ERR_TYPE.reference()), rs.getString(ERR_MSG.reference()), null, null));
        }
        return out;
    }

    public static ExceptionInfo getExceptionInfoIfNotNull(String className, String message, StackTraceRow[] stackTraceRows) {
        if(className != null || message != null) {
            return new ExceptionInfo(className, message, stackTraceRows, null);
        }
        return null;
    }
}
