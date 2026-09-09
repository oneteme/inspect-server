package org.usf.inspect.server.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.UUID;

import static java.util.Optional.ofNullable;
import static org.usf.inspect.server.JsonUtils.safeReadValue;
import static org.usf.inspect.server.Utils.fromNullableTimestamp;

public class Mappers {
    public static ResultSetMapper<InstanceEnvironment> instanceEnvironmentResultSetMapper(ObjectMapper mapper) {
        return rs -> {
            if (rs.next()) {
                var instanceEnvironment = new InstanceEnvironment(
                        rs.getObject("id", UUID.class),
                        fromNullableTimestamp(rs.getTimestamp("start")),
                        InstanceType.valueOf(rs.getString("type")),
                        rs.getString("appName"),
                        rs.getString("version"),
                        rs.getString("environement"),
                        rs.getString("address"),
                        rs.getString("os"),
                        rs.getString("re"),
                        rs.getString("user"),
                        rs.getString("branch"),
                        rs.getString("hash"),
                        rs.getString("collector"),
                        null,
                        safeReadValue(rs.getString("configuration"), InspectCollectorConfiguration.class, mapper)
                        //rs.getString(ADDITIONAL_PROPERTIES.reference()) != null ? mapper.readValue(rs.getString(ADDITIONAL_PROPERTIES.reference()), new TypeReference<Map<String, String>>() {}) : null,
                        //rs.getString(CONFIGURATION.reference()) != null ? mapper.readValue(rs.getString(CONFIGURATION.reference()), InspectCollectorConfiguration.class) : null
                );
                instanceEnvironment.setResource(safeReadValue(rs.getString("resource"), MachineResource.class, mapper));
                instanceEnvironment.setEnd(fromNullableTimestamp(rs.getTimestamp("end")));
                return instanceEnvironment;
            }
            return null;
        };
    }

    public static RowMapper<LogEntry> logEntryRowMapper(ObjectMapper mapper) {
        return (rs, row) -> {
            try {
                return new LogEntry(
                        fromNullableTimestamp(rs.getTimestamp("start")),
                        LogEntry.Level.valueOf(rs.getString("logLevel")),
                        rs.getString("logMessage"),
                        rs.getString("stacktrace") != null ? mapper.readValue(rs.getString("stacktrace"), new TypeReference<StackTraceRow[]>() {
                        }) : null
                );
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        };
    }

    public static RowMapper<TracePacket> instanceTraceRowMapper() {
        return (rs, row) ->
                new TracePacket(
                		fromNullableTimestamp(rs.getTimestamp("start")),
                		rs.getInt("attempts"),
//                        rs.getString("filename"),
                        rs.getObject("instanceEnv", UUID.class),
                        rs.getInt("traceCount"),
                        rs.getInt("pending"),
                        0
                );
    }

    public static RowMapper<MachineResourceUsage> machineResourceUsageRowMapper() {
        return (rs, row) ->
                new MachineResourceUsage(
                        fromNullableTimestamp(rs.getTimestamp("start")),
                        rs.getInt("usedHeap"),
                        rs.getInt("commitedHeap"),
                        rs.getInt("usedDiskSpace"),
                        0, // activeThreadCount
                        0, // startedThreadCount
                        0  // cpuUsage
                );
    }

    public static RestSessionDto defaultRestSession(ResultSet rs) throws SQLException {
        var restSession = new RestSessionDto();
        restSession.setId(rs.getObject("id", UUID.class));
        restSession.setMethod(rs.getString("method"));
        restSession.setProtocol(rs.getString("protocol"));
        restSession.setPath(rs.getString("path"));
        restSession.setQuery(rs.getString("query"));
        restSession.setStatus(rs.getShort("status"));
        restSession.setStart(fromNullableTimestamp(rs.getTimestamp("start")));
        restSession.setEnd(fromNullableTimestamp(rs.getTimestamp("end")));
        restSession.setName(rs.getString("apiName"));
        restSession.setUser(rs.getString("user"));
        return restSession;
    }

    public static RowMapper<RestSessionDto> restSessionRowMapper() {
        return (rs, row) -> {
            RestSessionDto out = defaultRestSession(rs);
            out.setAppName(rs.getString("appName"));
            return out;
        };
    }

    public static ResultSetMapper<RestSession> restSessionResultSetMapper(ObjectMapper mapper) {
        return rs->{
            if (rs.next()) {
                RestSession out = defaultRestSession(rs);
                out.setHost(rs.getString("host"));
                out.setPort(rs.getInt("port"));
                out.setContentType(rs.getString("media"));
                out.setAuthScheme(rs.getString("auth"));
                out.setInDataSize(rs.getLong("sizeIn"));
                out.setOutDataSize(rs.getLong("sizeOut"));
                out.setInContentEncoding(rs.getString("contentEncodingIn"));
                out.setOutContentEncoding(rs.getString("contentEncodingOut"));
                out.setThreadName(rs.getString("thread"));
                try {
                    out.setException(getExceptionInfoIfNotNull(rs.getString("errType"), rs.getString("errMsg"), rs.getString("stacktrace") != null ? mapper.readValue(rs.getString("stacktrace"), new TypeReference<StackTraceRow[]>() {
                    }) : null));
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
                out.setUserAgent(rs.getString("userAgt"));
                out.setRequestsMask(rs.getInt("mask"));
                out.setInstanceId(rs.getObject("instanceEnv", UUID.class));
                out.setCacheControl(rs.getString("cacheControl"));
                out.setLinked(rs.getBoolean("linked"));
                try {
                    String intermediateNodesStr = rs.getString("intermediateNodes");
                    if (intermediateNodesStr != null) { //TODO String[] => split(',')
                        out.setIntermediateNodes(mapper.readValue(intermediateNodesStr, new TypeReference<java.util.List<String>>() {}));
                    }
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
                return out;
            }
            return null;
        };
    }

    public static RowMapper<HttpSessionStage> restSessionStageRowMapper(){
        return (rs, row) -> {
            HttpSessionStage out = new HttpSessionStage(
                    rs.getObject("parent", UUID.class), // ou "requestId" selon alias SQL
                    rs.getInt("order")
            );
            out.setName(rs.getString("name"));
            out.setStart(fromNullableTimestamp(rs.getTimestamp("start")));
            out.setEnd(fromNullableTimestamp(rs.getTimestamp("end")));
            return out;
        };
    }

    public static RowMapper<RestSession> restSessionPulseRowMapper() {
        return (rs, row) -> {
            RestSession out = new RestSession();
            out.setId(rs.getObject("id", UUID.class));
            out.setMethod(rs.getString("method"));
            out.setPath(rs.getString("path"));
            out.setStatus(rs.getShort("status"));
            out.setStart(fromNullableTimestamp(rs.getTimestamp("start")));
            out.setEnd(fromNullableTimestamp(rs.getTimestamp("end")));
            out.setName(rs.getString("apiName"));
            out.setUser(rs.getString("user"));
            out.setHost(rs.getString("host"));
            out.setThreadName(rs.getString("thread"));
            return out;
        };
    }

    public static MainSessionDto defaultMainSession(ResultSet rs) throws SQLException {
        var mainSession = new MainSessionDto();
        mainSession.setId(rs.getObject("id", UUID.class));
        mainSession.setType(rs.getString("type"));
        mainSession.setStart(fromNullableTimestamp(rs.getTimestamp("start")));
        mainSession.setEnd(fromNullableTimestamp(rs.getTimestamp("end")));
        mainSession.setName(rs.getString("name"));
        mainSession.setUser(rs.getString("user"));
        mainSession.setLocation(rs.getString("location"));
        return mainSession;
    }

    public static RowMapper<MainSessionDto> mainSessionRowMapper(){
        return (rs, row) -> {
            MainSessionDto out = defaultMainSession(rs);
            out.setAddress(rs.getString("address"));
            out.setAppName(rs.getString("appName"));
            out.setStatus(rs.getInt("status"));
            return out;
        };
    }

    public static ResultSetMapper<MainSession> mainSessionResultSetMapper(ObjectMapper mapper) {
        return rs -> {
            if (rs.next()) {
                MainSession out = defaultMainSession(rs);
                out.setType(rs.getString("type"));
                out.setThreadName(rs.getString("thread"));
                try {
                    out.setException(getExceptionInfoIfNotNull(rs.getString("errType"), rs.getString("errMsg"), rs.getString("stacktrace") != null ? mapper.readValue(rs.getString("stacktrace"), new TypeReference<StackTraceRow[]>() {
                    }) : null));
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
                out.setInstanceId(rs.getObject("instanceEnv", UUID.class));
                out.setRequestsMask(rs.getInt("mask"));
                return out;
            }
            return null;
        };
    }

    public static RowMapper<MainSession> mainSessionPulseRowMapper(){
        return (rs, row) -> {
            MainSession out = new MainSession();
            out.setId(rs.getObject("id", UUID.class)); // add value of nullable
            out.setName(rs.getString("name"));
            out.setStart(fromNullableTimestamp(rs.getTimestamp("start")));
            out.setEnd(fromNullableTimestamp(rs.getTimestamp("end")));
            out.setType(rs.getString("type"));
            out.setLocation(rs.getString("location"));
            out.setThreadName(rs.getString("thread"));
            return out;
        };
    }

    public static RestRequestDto defaultRestRequest(ResultSet rs) throws SQLException {
        RestRequestDto out = new RestRequestDto();
        out.setId(rs.getObject("id", UUID.class));
        out.setSessionId(rs.getObject("parent", UUID.class));
        out.setProtocol(rs.getString("protocol"));
        out.setHost(rs.getString("host"));
        out.setPath(rs.getString("path"));
        out.setQuery(rs.getString("query"));
        out.setMethod(rs.getString("method"));
        out.setStatus(rs.getShort("status"));
        out.setStart(fromNullableTimestamp(rs.getTimestamp("start")));
        out.setEnd(fromNullableTimestamp(rs.getTimestamp("end")));
        out.setThreadName(rs.getString("thread"));
        out.setUser(rs.getString("user"));
        out.setBodyContent(rs.getString("bodyContent"));
        out.setLinked(rs.getBoolean("linked"));
        return out;
    }

    public static ResultSetMapper<RestRequest> restRequestResultSetMapper() {
        return rs -> {
            if (rs.next()) {
                var out = defaultRestRequest(rs);
                out.setPort(rs.getInt("port"));
                out.setInDataSize(rs.getLong("sizeIn"));
                out.setOutDataSize(rs.getLong("sizeOut"));
                out.setInContentEncoding(rs.getString("contentEncodingIn"));
                out.setOutContentEncoding(rs.getString("contentEncodingOut"));
                out.setAuthScheme(rs.getString("auth"));
                out.setInstanceId(rs.getObject("instanceEnv", UUID.class));
                return out;
            }
            return null;
        };
    }

    public static RowMapper<RestRequestDto> restRequestRowMapper() {
        return (rs, row) -> defaultRestRequest(rs);
    }

    public static RowMapper<HttpRequestStage> restRequestStageRowMapper(ObjectMapper mapper) {
        return (rs, row) -> {
            HttpRequestStage out = new HttpRequestStage(
                    rs.getObject("parent", UUID.class), // adapte au vrai alias SQL
                    rs.getInt("order")
            );
            out.setName(rs.getString("name"));
            out.setStart(fromNullableTimestamp(rs.getTimestamp("start")));
            out.setEnd(fromNullableTimestamp(rs.getTimestamp("end")));
            try {
                out.setException(getExceptionInfoIfNotNull(
                        rs.getString("errType"),
                        rs.getString("errMsg"),
                        rs.getString("stacktrace") != null
                                ? mapper.readValue(rs.getString("stacktrace"), new TypeReference<StackTraceRow[]>() {})
                                : null
                ));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
            return out;
        };
    }

    public static RowMapper<LocalRequest> localRequestRowMapper(){
        return (rs, row) -> {
            LocalRequest out = new LocalRequest();
            out.setId(rs.getObject("id", UUID.class));
            out.setName(rs.getString("name"));
            out.setLocation(rs.getString("location"));
            out.setStart(fromNullableTimestamp(rs.getTimestamp("start")));
            out.setEnd(fromNullableTimestamp(rs.getTimestamp("end")));
            out.setUser(rs.getString("user"));
            out.setThreadName(rs.getString("thread"));
            out.setType(rs.getString("type"));
            out.setException(getExceptionInfoIfNotNull(rs.getString("errType"), rs.getString("errMsg"), null));
            return out;
        };
    }

    private static DatabaseRequestDto defaultDatabaseRequest(ResultSet rs) throws SQLException {
        DatabaseRequestDto out = new DatabaseRequestDto();
        out.setId(rs.getObject("id", UUID.class));
        out.setSessionId(rs.getObject("parent", UUID.class));
        out.setHost(rs.getString("host"));
        out.setName(rs.getString("db"));
        out.setStart(fromNullableTimestamp(rs.getTimestamp("start")));
        out.setEnd(fromNullableTimestamp(rs.getTimestamp("end")));
        out.setUser(rs.getString("user"));
        out.setThreadName(rs.getString("thread"));
        out.setCommand(rs.getString("command"));
        out.setSchema(rs.getString("schema"));
        out.setProductName(rs.getString("dbName"));
        out.setFailed(rs.getBoolean("failed"));
        return out;
    }

    public static RowMapper<DatabaseRequestDto> databaseRequestRowMapper() {
        return  (rs, row) -> defaultDatabaseRequest(rs);
    }

    public static ResultSetMapper<DatabaseRequest> databaseRequestResultSetMapper() { // return null
        return rs -> {
            if (rs.next()) {
                DatabaseRequest out = defaultDatabaseRequest(rs);
                out.setDriverVersion(rs.getString("driver"));
                out.setProductVersion(rs.getString("dbVersion"));
                out.setPort(rs.getInt("port"));
                out.setInstanceId(rs.getObject("instanceEnv", UUID.class));
                return out;
            }
            return null;
        };
    }

    public static RowMapper<DatabaseRequestStage> databaseRequestStageRowMapper(ObjectMapper mapper){
        return (rs, row) -> {
            DatabaseRequestStage out = new DatabaseRequestStage(
                    rs.getObject("parent", UUID.class), // adapte au vrai alias SQL
                    rs.getInt("order")
            );
            out.setName(rs.getString("name"));
            out.setStart(fromNullableTimestamp(rs.getTimestamp("start")));
            out.setEnd(fromNullableTimestamp(rs.getTimestamp("end")));
            out.setCount(ofNullable(rs.getString("actionCount"))
                    .map(str -> Arrays.stream(str.split(",")).mapToLong(Long::parseLong).toArray())
                    .orElse(null));
            out.setCommand(rs.getString("command"));
            out.setArgs(ofNullable(rs.getString("arg"))
                    .map(str -> Arrays.stream(str.split(",")).toArray(String[]::new))
                    .orElse(null));
            try {
                out.setException(getExceptionInfoIfNotNull(
                        rs.getString("errType"),
                        rs.getString("errMsg"),
                        rs.getString("stacktrace") != null
                                ? mapper.readValue(rs.getString("stacktrace"), new TypeReference<StackTraceRow[]>() {})
                                : null
                ));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
            return out;
        };
    }

    private static FtpRequestDto defaultFtpRequest(ResultSet rs) throws SQLException {
        FtpRequestDto out = new FtpRequestDto();
        out.setId(rs.getObject("id", UUID.class));
        out.setSessionId(rs.getObject("parent", UUID.class));
        out.setHost(rs.getString("host"));
        out.setStart(fromNullableTimestamp(rs.getTimestamp("start")));
        out.setEnd(fromNullableTimestamp(rs.getTimestamp("end")));
        out.setThreadName(rs.getString("thread"));
        out.setUser(rs.getString("user"));
        out.setFailed(rs.getBoolean("failed"));
        out.setCommand(rs.getString("command"));
        return out;
    }

    public static RowMapper<FtpRequestDto> ftpRequestRowMapper(){
        return (rs, row) -> defaultFtpRequest(rs);
    }

    public static ResultSetMapper<FtpRequest> ftpRequestResultSetMapper() {
        return rs -> {
            if (rs.next()) {
                FtpRequest out = defaultFtpRequest(rs);
                out.setPort(rs.getInt("port"));
                out.setProtocol(rs.getString("protocol"));
                out.setServerVersion(rs.getString("serverVersion"));
                out.setClientVersion(rs.getString("clientVersion"));
                out.setInstanceId(rs.getObject("instanceEnv", UUID.class));
                return out;
            }
            return null;
        };
    }

    public static RowMapper<FtpRequestStage> ftpRequestStageRowMapper(ObjectMapper mapper){
        return (rs, row) -> {
            FtpRequestStage out = new FtpRequestStage(
                    rs.getObject("parent", UUID.class), // adapte au vrai alias SQL
                    rs.getInt("order")
            );
            out.setName(rs.getString("name"));
            out.setStart(fromNullableTimestamp(rs.getTimestamp("start")));
            out.setEnd(fromNullableTimestamp(rs.getTimestamp("end")));
            out.setCommand(rs.getString("command"));
            out.setArgs(ofNullable(rs.getString("arg"))
                    .map(str -> Arrays.stream(str.split(",")).toArray(String[]::new))
                    .orElse(null));
            try {
                out.setException(getExceptionInfoIfNotNull(
                        rs.getString("errType"),
                        rs.getString("errMsg"),
                        rs.getString("stacktrace") != null
                                ? mapper.readValue(rs.getString("stacktrace"), new TypeReference<StackTraceRow[]>() {})
                                : null
                ));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
            return out;
        };
    }

    private static MailRequestDto defaultSmtpRequest(ResultSet rs) throws SQLException{
        MailRequestDto out = new MailRequestDto();
        out.setId(rs.getObject("id", UUID.class));
        out.setSessionId(rs.getObject("parent", UUID.class));
        out.setHost(rs.getString("host"));
        out.setStart(fromNullableTimestamp(rs.getTimestamp("start")));
        out.setEnd(fromNullableTimestamp(rs.getTimestamp("end")));
        out.setThreadName(rs.getString("thread"));
        out.setUser(rs.getString("user"));
        out.setFailed(rs.getBoolean("failed"));
        out.setCommand(rs.getString("command"));
        return out;
    }

    public static RowMapper<MailRequestDto> smtpRequestRowMapper(){
        return (rs, row) -> defaultSmtpRequest(rs);
    }

    public static ResultSetMapper<MailRequest> smtpRequestResultSetMapper() {
        return rs -> {
            if (rs.next()) {
                MailRequest out = defaultSmtpRequest(rs);
                out.setPort(rs.getInt("port"));
                out.setInstanceId(rs.getObject("instanceEnv", UUID.class));
                return out;
            }
            return null;
        };
    }

    public static RowMapper<MailRequestStage> smtpRequestStageRowMapper(ObjectMapper mapper){
        return (rs, row) -> {
            MailRequestStage out = new MailRequestStage(
                    rs.getObject("parent", UUID.class),
                    rs.getInt("order")
            );
            out.setName(rs.getString("name"));
            out.setCommand(rs.getString("command"));
            out.setStart(fromNullableTimestamp(rs.getTimestamp("start")));
            out.setEnd(fromNullableTimestamp(rs.getTimestamp("end")));
            try {
                out.setException(getExceptionInfoIfNotNull(
                        rs.getString("errType"),
                        rs.getString("errMsg"),
                        rs.getString("stacktrace") != null
                                ? mapper.readValue(rs.getString("stacktrace"), new TypeReference<StackTraceRow[]>() {})
                                : null
                ));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
            return out;
        };
    }

    public static RowMapper<Mail> smtpRequestMailRowMapper(){
        return (rs, row) -> {
            var out = new Mail();
            out.setContentType(rs.getString("media"));
            out.setFrom(ofNullable(rs.getString("from")).map(str -> Arrays.stream(str.split(",")).toArray(String[]::new)).orElse(null));
            out.setRecipients(ofNullable(rs.getString("recipients")).map(str -> Arrays.stream(str.split(",")).toArray(String[]::new)).orElse(null));
            out.setReplyTo(ofNullable(rs.getString("replyTo")).map(str -> Arrays.stream(str.split(",")).toArray(String[]::new)).orElse(null));
            out.setSize(rs.getInt("size"));
            out.setSubject(rs.getString("subject"));
            return out;
        };
    }

    private static DirectoryRequestDto defaultLdapRequest(ResultSet rs) throws SQLException{
        DirectoryRequestDto out = new DirectoryRequestDto();
        out.setId(rs.getObject("id", UUID.class));
        out.setHost(rs.getString("host"));
        out.setStart(fromNullableTimestamp(rs.getTimestamp("start")));
        out.setEnd(fromNullableTimestamp(rs.getTimestamp("end")));
        out.setThreadName(rs.getString("thread"));
        out.setUser(rs.getString("user"));
        out.setFailed(rs.getBoolean("failed"));
        out.setCommand(rs.getString("command"));
        return out;
    }

    public static RowMapper<DirectoryRequestDto> ldapRequestRowMapper(){
        return (rs, row) -> defaultLdapRequest(rs);
    }

    public static ResultSetMapper<DirectoryRequest> ldapRequestResultSetMapper() {
        return rs -> {
            if (rs.next()) {
                DirectoryRequest out = defaultLdapRequest(rs);
                out.setSessionId(rs.getObject("parent", UUID.class));
                out.setPort(rs.getInt("port"));
                out.setProtocol(rs.getString("protocol"));
                out.setInstanceId(rs.getObject("instanceEnv", UUID.class));
                return out;
            }
            return null;
        };

    }

    public static RowMapper<DirectoryRequestStage> ldapRequestStageRowMapper(ObjectMapper mapper){
        return (rs, row) -> {
            var out = new DirectoryRequestStage(
                    rs.getObject("parent", UUID.class), // adapte au vrai alias SQL
                    rs.getInt("order")
            );
            out.setName(rs.getString("name"));
            out.setCommand(rs.getString("command"));
            out.setStart(fromNullableTimestamp(rs.getTimestamp("start")));
            out.setEnd(fromNullableTimestamp(rs.getTimestamp("end")));
            try {
                out.setException(getExceptionInfoIfNotNull(
                        rs.getString("errType"),
                        rs.getString("errMsg"),
                        rs.getString("stacktrace") != null
                                ? mapper.readValue(rs.getString("stacktrace"), new TypeReference<StackTraceRow[]>() {})
                                : null
                ));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
            return out;
        };
    }

    public static ResultSetMapper<Map<Long, ExceptionTrace>> exceptionByRequestResultSetMapper() {
        return rs -> {
            Map<Long, ExceptionTrace> out = new HashMap<>();
            while(rs.next()) {
                out.put(rs.getLong("parent"), new ExceptionTrace(rs.getString("errType"), rs.getString("errMsg"), null, null));
            }
            return out;
        };
    }

    public static RowMapper<UserAction> userActionRowMapper(){
        return (rs, row) -> {
            UserAction out = new UserAction(
                    rs.getString("name"),
                    rs.getString("nodeName"),
                    rs.getString("type"),
                    fromNullableTimestamp(rs.getTimestamp("start"))
            );
            out.setCdSession(rs.getString("parent"));
            return out;
        };
    }

    public static ExceptionTrace getExceptionInfoIfNotNull(String className, String message, StackTraceRow[] stackTraceRows) {
        if (className != null || message != null) {
            return new ExceptionTrace(className, message, stackTraceRows, null);
        }
        return null;
    }
}
