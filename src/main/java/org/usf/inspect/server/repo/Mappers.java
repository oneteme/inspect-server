package org.usf.inspect.server.repo;

import static org.usf.inspect.server.JsonUtils.safeReadValue;
import static org.usf.inspect.server.Utils.fromNullableTimestamp;
import static org.usf.jquery.core.ResultSetMapper.columnNames;

import java.util.Arrays;

import org.usf.inspect.core.ExceptionInfo;
import org.usf.inspect.core.InspectCollectorConfiguration;
import org.usf.inspect.core.InstanceEnvironment;
import org.usf.inspect.core.InstanceType;
import org.usf.inspect.core.MachineResource;
import org.usf.inspect.core.StackTraceRow;
import org.usf.inspect.server.model.MainSession;
import org.usf.jquery.core.ResultSetMapper;
import org.usf.jquery.core.RowMapper;
import org.usf.inspect.core.LogEntry;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Mappers {
    public static ResultSetMapper<InstanceEnvironment> instanceEnvironmentMapper(ObjectMapper mapper) {
        return rs->{
            if(rs.next()) {
                var instanceEnvironment = new InstanceEnvironment(
                        rs.getString("id"),
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
                        safeReadValue(rs.getString("configuration"), mapper, InspectCollectorConfiguration.class)
                        //rs.getString(ADDITIONAL_PROPERTIES.reference()) != null ? mapper.readValue(rs.getString(ADDITIONAL_PROPERTIES.reference()), new TypeReference<Map<String, String>>() {}) : null,
                        //rs.getString(CONFIGURATION.reference()) != null ? mapper.readValue(rs.getString(CONFIGURATION.reference()), InspectCollectorConfiguration.class) : null
                );
                instanceEnvironment.setResource(safeReadValue(rs.getString("resource"), mapper, MachineResource.class));
                instanceEnvironment.setEnd(fromNullableTimestamp(rs.getTimestamp("end")));
                return instanceEnvironment;
            }
            return null;
        };
    }
    
    public static ResultSetMapper<MainSession> createBaseMainSession(ObjectMapper mapper) {
        return rs-> {
        	System.out.print("ResultSet main session : "+Arrays.toString(columnNames(rs)) );
            if (rs.next()) {
            	MainSession out = new MainSession();

                out.setId(rs.getString("id"));
                out.setName(rs.getString("name"));
                out.setStart(fromNullableTimestamp(rs.getTimestamp("start")));
                out.setEnd(fromNullableTimestamp(rs.getTimestamp("end")));
                out.setType(rs.getString("type"));
                out.setLocation(rs.getString("location"));
                out.setThreadName(rs.getString("thread"));
                try {
                    out.setException(getExceptionInfoIfNotNull(rs.getString("errorType"), rs.getString("errorMessage"), rs.getString("stacktrace") != null ? mapper.readValue(rs.getString("stacktrace"), new TypeReference<StackTraceRow[]>() {
                    }) : null));
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
                out.setUser(rs.getString("user"));
                out.setInstanceId(rs.getString("instanceEnv"));
                out.setRequestsMask(rs.getInt("mask"));
                return out;
            }
            return null;
        };
    }
    
    public static ExceptionInfo getExceptionInfoIfNotNull(String className, String message, StackTraceRow[] stackTraceRows) {
        if(className != null || message != null) {
            return new ExceptionInfo(className, message, stackTraceRows, null);
        }
        return null;
    }
    
  public static RowMapper<LogEntry> instanceLogEntryMapper(ObjectMapper mapper) {
  return (rs,row) -> {
      try {
          return new LogEntry(
                  fromNullableTimestamp(rs.getTimestamp("start")),
                  LogEntry.Level.valueOf(rs.getString("logLevel")),
                  rs.getString("logMessage"),
                  rs.getString("stacktrace") != null ? mapper.readValue(rs.getString("stacktrace"), new TypeReference<StackTraceRow[]>() {}) : null
          );
      } catch (JsonProcessingException e) {
          throw new RuntimeException(e);
      }
  };
}
}
