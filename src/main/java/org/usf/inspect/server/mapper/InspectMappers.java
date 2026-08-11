package org.usf.inspect.server.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.usf.inspect.core.*;
import org.usf.inspect.server.dto.*;
import org.usf.inspect.server.model.*;
import org.usf.jquery.core.ResultSetMapper;
import org.usf.jquery.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static java.util.Optional.ofNullable;
import static org.usf.inspect.server.JsonUtils.*;
import static org.usf.inspect.server.Utils.fromNullableTimestamp;
import static org.usf.inspect.server.config.TraceApiColumn.*;

/**
 * Provides result set and row mappers for inspect server domain objects and DTOs.
 */
@Slf4j
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class InspectMappers {

    /**
     * Creates a mapper that reads a single instance environment from a result set.
     *
     * @param mapper the object mapper used to deserialize JSON columns
     * @return a result set mapper for instance environments
     */
    public static ResultSetMapper<InstanceEnvironment> instanceEnvironmentMapper(ObjectMapper mapper) {
        return rs->{
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
                        safeReadValue(rs.getString(CONFIGURATION.reference()), mapper, InspectCollectorConfiguration.class)
                        //rs.getString(ADDITIONAL_PROPERTIES.reference()) != null ? mapper.readValue(rs.getString(ADDITIONAL_PROPERTIES.reference()), new TypeReference<Map<String, String>>() {}) : null,
                        //rs.getString(CONFIGURATION.reference()) != null ? mapper.readValue(rs.getString(CONFIGURATION.reference()), InspectCollectorConfiguration.class) : null
                );
                instanceEnvironment.setResource(safeReadValue(rs.getString(RESOURCE.reference()), mapper, MachineResource.class));
                instanceEnvironment.setEnd(fromNullableTimestamp(rs.getTimestamp(END.reference())));
                return instanceEnvironment;
            }
            return null;
        };
    }

    /**
     * Creates a row mapper for instance trace records.
     *
     * @return a row mapper for instance traces
     */
    public static RowMapper<InstanceTrace> instanceTraceMapper() {
        return rs ->
            new InstanceTrace(
                    rs.getInt(PENDING.reference()),
                    rs.getString(FILENAME.reference()),
                    fromNullableTimestamp(rs.getTimestamp(START.reference())),
                    rs.getString(INSTANCE_ENV.reference()),
                    rs.getInt(TRACE_COUNT.reference()),
                    rs.getInt(ATTEMPTS.reference())
            );
    }

    /**
     * Creates a row mapper for machine resource usage records.
     *
     * @return a row mapper for machine resource usage entries
     */
    public static RowMapper<MachineResourceUsage> instanceResourceUsageMapper() {
        return rs ->
                new MachineResourceUsage(
                        fromNullableTimestamp(rs.getTimestamp(START.reference())),
                        rs.getInt(USED_HEAP.reference()),
                        rs.getInt(COMMITED_HEAP.reference()),
                        rs.getInt(USED_DISK_SPACE.reference())
                );
    }

    /**
     * Creates a row mapper for log entries.
     *
     * @param mapper the object mapper used to deserialize stack traces
     * @return a row mapper for log entries
     */
    public static RowMapper<LogEntry> instanceLogEntryMapper(ObjectMapper mapper) {
        return rs -> {
            try {
                return new LogEntry(
                        fromNullableTimestamp(rs.getTimestamp(START.reference())),
                        LogEntry.Level.valueOf(rs.getString(LOG_LEVEL.reference())),
                        rs.getString(LOG_MESSAGE.reference()),
                        rs.getString(STACKTRACE.reference()) != null ? mapper.readValue(rs.getString(STACKTRACE.reference()), new TypeReference<StackTraceRow[]>() {}) : null
                );
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        };
    }

    /**
     * Creates a lightweight row mapper for REST request DTOs.
     *
     * @return a row mapper for REST request DTOs
     */
    public static RowMapper<RestRequestDto> restRequestLazyMapper() {
        return rs -> {
            RestRequestDto out = createBaseRestRequest(rs);
            out.setException(getExceptionInfoIfNotNull(rs.getString(ERR_TYPE.reference()), rs.getString(ERR_MSG.reference()), null));
            return out;
        };
    }

    /**
     * Creates the base REST request DTO from the current result set row.
     *
     * @param rs the result set positioned on the row to read
     * @return the populated base REST request DTO
     * @throws SQLException if the result set cannot be read
     */
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
        out.setUser(rs.getString(USER.reference()));
        out.setBodyContent(rs.getString(BODY_CONTENT.reference()));
        out.setLinked(rs.getBoolean(LINKED.reference()));
        return out;
    }

    /**
     * Reads a complete REST request from the result set.
     *
     * @param rs the result set containing the REST request data
     * @return the populated REST request, or {@code null} when the result set is empty
     * @throws SQLException if the result set cannot be read
     */
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
            out.setInstanceId(rs.getString(INSTANCE_ENV.reference()));
            return out;
        }
        return null;
    }
    
    /**
     * Creates a shallow row mapper for REST session DTOs.
     *
     * @return a row mapper for shallow REST session DTOs
     */
    public static RowMapper<RestSessionDto> restSessionShallowMapper() {
        return rs -> {
            RestSessionDto out = defaultRestSessionMapper(rs);
            out.setAppName(rs.getString(APP_NAME.reference()));
            out.setException(getExceptionInfoIfNotNull(rs.getString(ERR_TYPE.reference()), rs.getString(ERR_MSG.reference()), null));
            return out;
        };
    }

    /**
     * Creates a row mapper for REST session pulse views.
     *
     * @return a row mapper for REST session pulse data
     */
    public static RowMapper<RestSession> restSessionPulseRowMapper() {
        return rs -> {
            RestSession out = new RestSession();
            out.setId(rs.getString(ID.reference()));
            out.setMethod(rs.getString(METHOD.reference()));
            out.setPath(rs.getString(PATH.reference()));
            out.setStatus(rs.getInt(STATUS.reference()));
            out.setStart(fromNullableTimestamp(rs.getTimestamp(START.reference())));
            out.setEnd(fromNullableTimestamp(rs.getTimestamp(END.reference())));
            out.setName(rs.getString(API_NAME.reference()));
            out.setUser(rs.getString(USER.reference()));
            out.setHost(rs.getString(HOST.reference()));
            out.setThreadName(rs.getString(THREAD.reference()));
            return out;
        };
    }

    /**
     * Creates the base REST session DTO from the current result set row.
     *
     * @param rs the result set positioned on the row to read
     * @return the populated base REST session DTO
     * @throws SQLException if the result set cannot be read
     */
    public static RestSessionDto defaultRestSessionMapper(ResultSet rs) throws SQLException {
        var restSession = new RestSessionDto();
        restSession.setId(rs.getString(ID.reference()));
        restSession.setMethod(rs.getString(METHOD.reference()));
        restSession.setProtocol(rs.getString(PROTOCOL.reference()));
        restSession.setPath(rs.getString(PATH.reference()));
        restSession.setQuery(rs.getString(QUERY.reference()));
        restSession.setStatus(rs.getInt(STATUS.reference()));
        restSession.setInDataSize(rs.getLong(SIZE_IN.reference()));
        restSession.setOutDataSize(rs.getLong(SIZE_OUT.reference()));
        restSession.setStart(fromNullableTimestamp(rs.getTimestamp(START.reference())));
        restSession.setEnd(fromNullableTimestamp(rs.getTimestamp(END.reference())));
        restSession.setName(rs.getString(API_NAME.reference()));
        restSession.setUser(rs.getString(USER.reference()));
        return restSession;
    }

    /**
     * Creates a mapper that reads a single REST session from a result set.
     *
     * @param mapper the object mapper used to deserialize stack traces
     * @return a result set mapper for REST sessions
     */
    public static ResultSetMapper<RestSession> restSessionResultSetMapper(ObjectMapper mapper) {
        return rs->{
            if (rs.next()) {
                RestSession out = defaultRestSessionMapper(rs);
                out.setHost(rs.getString(HOST.reference()));
                out.setPort(rs.getInt(PORT.reference()));
                out.setContentType(rs.getString(MEDIA.reference()));
                out.setAuthScheme(rs.getString(AUTH.reference()));
                out.setInContentEncoding(rs.getString(CONTENT_ENCODING_IN.reference()));
                out.setOutContentEncoding(rs.getString(CONTENT_ENCODING_OUT.reference()));
                out.setThreadName(rs.getString(THREAD.reference()));
                try {
                    out.setException(getExceptionInfoIfNotNull(rs.getString(ERR_TYPE.reference()), rs.getString(ERR_MSG.reference()), rs.getString(STACKTRACE.reference()) != null ? mapper.readValue(rs.getString(STACKTRACE.reference()), new TypeReference<StackTraceRow[]>() {
                    }) : null));
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
                out.setUserAgent(rs.getString(USER_AGT.reference()));
                out.setRequestsMask(rs.getInt(MASK.reference()));
                out.setInstanceId(rs.getString(INSTANCE_ENV.reference()));
                out.setCacheControl(rs.getString(CACHE_CONTROL.reference()));
                out.setLinked(rs.getBoolean(LINKED.reference()));
                return out;
            }
            return null;
        };
    }

    /**
     * Creates a mapper that reads a single main session from a result set.
     *
     * @param mapper the object mapper used to deserialize stack traces
     * @return a result set mapper for main sessions
     */
    public static ResultSetMapper<MainSession> createBaseMainSession(ObjectMapper mapper) {
        return rs-> {
            if (rs.next()) {
                MainSession out = new MainSession();
                out.setId(rs.getString(ID.reference()));
                out.setName(rs.getString(NAME.reference()));
                out.setStart(fromNullableTimestamp(rs.getTimestamp(START.reference())));
                out.setEnd(fromNullableTimestamp(rs.getTimestamp(END.reference())));
                out.setType(rs.getString(TYPE.reference()));
                out.setLocation(rs.getString(LOCATION.reference()));
                out.setThreadName(rs.getString(THREAD.reference()));
                try {
                    out.setException(getExceptionInfoIfNotNull(rs.getString(ERR_TYPE.reference()), rs.getString(ERR_MSG.reference()), rs.getString(STACKTRACE.reference()) != null ? mapper.readValue(rs.getString(STACKTRACE.reference()), new TypeReference<StackTraceRow[]>() {
                    }) : null));
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
                out.setUser(rs.getString(USER.reference()));
                out.setInstanceId(rs.getString(INSTANCE_ENV.reference()));
                out.setRequestsMask(rs.getInt(MASK.reference()));
                return out;
            }
            return null;
        };
    }
    
    /**
     * Creates a row mapper for main session pulse views.
     *
     * @return a row mapper for main session pulse data
     */
    public static RowMapper<MainSession> mainSessionPulseRowMapper(){
        return rs -> {
            MainSession out = new MainSession();
            out.setId(rs.getString(ID.reference())); // add value of nullable
            out.setName(rs.getString(NAME.reference()));
            out.setStart(fromNullableTimestamp(rs.getTimestamp(START.reference())));
            out.setEnd(fromNullableTimestamp(rs.getTimestamp(END.reference())));
            out.setType(rs.getString(TYPE.reference()));
            out.setLocation(rs.getString(LOCATION.reference()));
            out.setThreadName(rs.getString(THREAD.reference()));
            return out;
        };
    }
    
    /**
     * Creates a row mapper for searchable main session DTOs.
     *
     * @return a row mapper for searchable main session DTOs
     */
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
            out.setAddress(rs.getString(ADDRESS.reference()));
            return out;
        };
    }

    /**
     * Creates a row mapper for local requests.
     *
     * @return a row mapper for local requests
     */
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

    /**
     * Creates a lightweight row mapper for database request DTOs.
     *
     * @return a row mapper for database request DTOs
     */
    public static RowMapper<DatabaseRequestDto> databaseRequestLazyMapper() {
        return rs -> {
            DatabaseRequestDto out = createBaseDatabaseRequest(rs);
            out.setException(getExceptionInfoIfNotNull(rs.getString(ERR_TYPE.reference()), rs.getString(ERR_MSG.reference()), null));
            return out;
        };
    }

    /**
     * Reads a complete database request from the result set.
     *
     * @param rs the result set containing the database request data
     * @return the populated database request, or {@code null} when the result set is empty
     * @throws SQLException if the result set cannot be read
     */
    public static DatabaseRequest databaseRequestComplete(ResultSet rs) throws SQLException { // return null
        if (rs.next()) {
            DatabaseRequest out = createBaseDatabaseRequest(rs);
            out.setDriverVersion(rs.getString(DRIVER.reference()));
            out.setProductVersion(rs.getString(DB_VERSION.reference()));
            out.setPort(rs.getInt(PORT.reference()));
            out.setInstanceId(rs.getString(INSTANCE_ENV.reference()));
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
        out.setProductName(rs.getString(DB_NAME.reference()));
        out.setFailed(rs.getBoolean(FAILED.reference()));
        return out;
    }



    /**
     * Creates a row mapper for database request stages.
     *
     * @param mapper the object mapper used to deserialize stack traces
     * @return a row mapper for database request stages
     */
    public static RowMapper<DatabaseRequestStage> databaseRequestStageMapper(ObjectMapper mapper){
        return rs -> {
            DatabaseRequestStage out= new DatabaseRequestStage();
            out.setName(rs.getString(NAME.reference()));
            out.setStart(fromNullableTimestamp(rs.getTimestamp(START.reference())));
            out.setEnd(fromNullableTimestamp(rs.getTimestamp(END.reference())));
            out.setCount(ofNullable(rs.getString(ACTION_COUNT.reference())).map(str -> Arrays.stream(str.split(",")).mapToLong(Long::parseLong).toArray()).orElse(null));
            out.setCommand(rs.getString(COMMAND.reference()));
            out.setArgs(ofNullable(rs.getString(ARG.reference())).map(str -> Arrays.stream(str.split(",")).toArray(String[]::new)).orElse(null));
            try {
                out.setException(getExceptionInfoIfNotNull(rs.getString(ERR_TYPE.reference()), rs.getString(ERR_MSG.reference()), rs.getString(STACKTRACE.reference()) != null ? mapper.readValue(rs.getString(STACKTRACE.reference()), new TypeReference<StackTraceRow[]>() {}) : null));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
            out.setOrder(rs.getInt(ORDER.reference()));
            return out;
        };
    }

    /**
     * Creates a row mapper for REST request stages.
     *
     * @param mapper the object mapper used to deserialize stack traces
     * @return a row mapper for REST request stages
     */
    public static RowMapper<HttpRequestStage> restRequestStageMapper(ObjectMapper mapper) {
        return rs -> {
            HttpRequestStage out= new HttpRequestStage();
            out.setName(rs.getString(NAME.reference()));
            out.setStart(fromNullableTimestamp(rs.getTimestamp(START.reference())));
            out.setEnd(fromNullableTimestamp(rs.getTimestamp(END.reference())));
            try {
                out.setException(getExceptionInfoIfNotNull(rs.getString(ERR_TYPE.reference()), rs.getString(ERR_MSG.reference()), rs.getString(STACKTRACE.reference()) != null ? mapper.readValue(rs.getString(STACKTRACE.reference()), new TypeReference<StackTraceRow[]>() {}) : null));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
            out.setOrder(rs.getInt(ORDER.reference()));
            return out;
        };
    }

    /**
     * Creates a row mapper for REST session stages.
     *
     * @return a row mapper for REST session stages
     */
    public static RowMapper<HttpSessionStage> restSessionStageMapper(){
        return rs -> {
            HttpSessionStage out= new HttpSessionStage();
            out.setName(rs.getString(NAME.reference()));
            out.setStart(fromNullableTimestamp(rs.getTimestamp(START.reference())));
            out.setEnd(fromNullableTimestamp(rs.getTimestamp(END.reference())));
            out.setOrder(rs.getInt(ORDER.reference()));
            return out;
        };
    }

    /**
     * Creates a lightweight row mapper for FTP request DTOs.
     *
     * @return a row mapper for FTP request DTOs
     */
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
        out.setUser(rs.getString(USER.reference()));
        out.setFailed(rs.getBoolean(FAILED.reference()));
        out.setCommand(rs.getString(COMMAND.reference()));
        return out;
    }

    /**
     * Reads a complete FTP request from the result set.
     *
     * @param rs the result set containing the FTP request data
     * @return the populated FTP request, or {@code null} when the result set is empty
     * @throws SQLException if the result set cannot be read
     */
    public static FtpRequest ftpRequestComplete(ResultSet rs) throws SQLException {
        if (rs.next()) {
            FtpRequest out = createBaseFtpRequest(rs);
            out.setSessionId(rs.getString(PARENT.reference()));
            out.setPort(rs.getInt(PORT.reference()));
            out.setProtocol(rs.getString(PROTOCOL.reference()));
            out.setServerVersion(rs.getString(SERVER_VERSION.reference()));
            out.setClientVersion(rs.getString(CLIENT_VERSION.reference()));
            out.setInstanceId(rs.getString(INSTANCE_ENV.reference()));
            return out;
        }
        return null;
    }
    
    /**
     * Creates a row mapper for FTP request stages.
     *
     * @param mapper the object mapper used to deserialize stack traces
     * @return a row mapper for FTP request stages
     */
    public static RowMapper<FtpRequestStage> ftpRequestStageMapper(ObjectMapper mapper){
        return rs -> {
            FtpRequestStage out = new FtpRequestStage();
            out.setName(rs.getString(NAME.reference()));
            out.setStart(fromNullableTimestamp(rs.getTimestamp(START.reference())));
            out.setEnd(fromNullableTimestamp(rs.getTimestamp(END.reference())));
            out.setCommand(rs.getString(COMMAND.reference()));
            out.setArgs(ofNullable(rs.getString(ARG.reference())).map(str -> Arrays.stream(str.split(",")).toArray(String[]::new)).orElse(null));
            try {
                out.setException(getExceptionInfoIfNotNull(rs.getString(ERR_TYPE.reference()), rs.getString(ERR_MSG.reference()), rs.getString(STACKTRACE.reference()) != null ? mapper.readValue(rs.getString(STACKTRACE.reference()), new TypeReference<StackTraceRow[]>() {}) : null));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
            out.setOrder(rs.getInt(ORDER.reference()));
            return out;
        };
    }

    /**
     * Creates a lightweight row mapper for mail request DTOs.
     *
     * @return a row mapper for mail request DTOs
     */
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
        out.setUser(rs.getString(USER.reference()));
        out.setFailed(rs.getBoolean(FAILED.reference()));
        out.setCommand(rs.getString(COMMAND.reference()));
        return out;
    }

    /**
     * Reads a complete mail request from the result set.
     *
     * @param rs the result set containing the mail request data
     * @return the populated mail request, or {@code null} when the result set is empty
     * @throws SQLException if the result set cannot be read
     */
    public static MailRequest mailRequestCompleteMapper(ResultSet rs) throws SQLException {
        if (rs.next()) {
            MailRequest out = createBaseMailRequest(rs);
            out.setSessionId(rs.getString(PARENT.reference()));
            out.setPort(rs.getInt(PORT.reference()));
            out.setInstanceId(rs.getString(INSTANCE_ENV.reference()));
            return out;
        }
        return null;
    }

    /**
     * Creates a row mapper for mail request stages.
     *
     * @param mapper the object mapper used to deserialize stack traces
     * @return a row mapper for mail request stages
     */
    public static RowMapper<MailRequestStage> mailRequestStageMapper(ObjectMapper mapper){
        return rs -> {
            MailRequestStage out = new MailRequestStage();
            out.setName(rs.getString(NAME.reference()));
            out.setCommand(rs.getString(COMMAND.reference()));
            out.setStart(fromNullableTimestamp(rs.getTimestamp(START.reference())));
            out.setEnd(fromNullableTimestamp(rs.getTimestamp(END.reference())));
            try {
                out.setException(getExceptionInfoIfNotNull(rs.getString(ERR_TYPE.reference()), rs.getString(ERR_MSG.reference()), rs.getString(STACKTRACE.reference()) != null ? mapper.readValue(rs.getString(STACKTRACE.reference()), new TypeReference<StackTraceRow[]>() {}) : null));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
            out.setOrder(rs.getInt(ORDER.reference()));
            return out;
        };
    }
    
    /**
     * Creates a row mapper for mail details.
     *
     * @return a row mapper for mail records
     */
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

    /**
     * Creates a lightweight row mapper for directory request DTOs.
     *
     * @return a row mapper for directory request DTOs
     */
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
        out.setUser(rs.getString(USER.reference()));
        out.setFailed(rs.getBoolean(FAILED.reference()));
        out.setCommand(rs.getString(COMMAND.reference()));
        return out;
    }

    /**
     * Reads a complete directory request from the result set.
     *
     * @param rs the result set containing the directory request data
     * @return the populated directory request, or {@code null} when the result set is empty
     * @throws SQLException if the result set cannot be read
     */
    public static DirectoryRequest ldapRequestCompleteMapper(ResultSet rs) throws SQLException {
        if (rs.next()) {
            DirectoryRequest out = createBaseLdapRequest(rs);
            out.setSessionId(rs.getString(PARENT.reference()));
            out.setPort(rs.getInt(PORT.reference()));
            out.setProtocol(rs.getString(PROTOCOL.reference()));
            out.setInstanceId(rs.getString(INSTANCE_ENV.reference()));
            return out;
        }
        return null;
    }
    
    /**
     * Creates a row mapper for directory request stages.
     *
     * @param mapper the object mapper used to deserialize stack traces
     * @return a row mapper for directory request stages
     */
    public static RowMapper<DirectoryRequestStage> ldapRequestStageMapper(ObjectMapper mapper){
        return rs -> {
            var out = new DirectoryRequestStage();
            out.setName(rs.getString(NAME.reference()));
            out.setCommand(rs.getString(COMMAND.reference()));
            out.setStart(fromNullableTimestamp(rs.getTimestamp(START.reference())));
            out.setEnd(fromNullableTimestamp(rs.getTimestamp(END.reference())));
            try {
                out.setException(getExceptionInfoIfNotNull(rs.getString(ERR_TYPE.reference()), rs.getString(ERR_MSG.reference()), rs.getString(STACKTRACE.reference()) != null ? mapper.readValue(rs.getString(STACKTRACE.reference()), new TypeReference<StackTraceRow[]>() {}) : null));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
            out.setOrder(rs.getInt(ORDER.reference()));
            return out;
        };
    }

    /**
     * Creates a row mapper for user actions.
     *
     * @return a row mapper for user actions
     */
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

    /**
     * Reads exception information indexed by parent identifier from the result set.
     *
     * @param rs the result set containing exception information rows
     * @return a map of parent identifiers to exception information
     * @throws SQLException if the result set cannot be read
     */
    public static Map<Long, ExceptionInfo> exceptionInfoMapper(ResultSet rs) throws SQLException {
        Map<Long, ExceptionInfo> out = new HashMap<>();
        while(rs.next()) {
            out.put(rs.getLong(PARENT.reference()), new ExceptionInfo(rs.getString(ERR_TYPE.reference()), rs.getString(ERR_MSG.reference()), null, null));
        }
        return out;
    }

    /**
     * Creates exception information only when an exception class name or message is present.
     *
     * @param className the exception class name
     * @param message the exception message
     * @param stackTraceRows the stack trace rows associated with the exception
     * @return the exception information, or {@code null} when no exception data is available
     */
    public static ExceptionInfo getExceptionInfoIfNotNull(String className, String message, StackTraceRow[] stackTraceRows) {
        if(className != null || message != null) {
            return new ExceptionInfo(className, message, stackTraceRows, null);
        }
        return null;
    }
}
