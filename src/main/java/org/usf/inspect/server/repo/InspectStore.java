package org.usf.inspect.server.repo;

import static org.usf.inspect.server.InspectApplication.defaultMapper;
import static org.usf.inspect.server.JsonUtils.safeReadValue;
import static org.usf.inspect.server.Utils.fromNullableTimestamp;
import static org.usf.inspect.server.config.TraceApiColumn.END;
import static org.usf.inspect.server.config.TraceApiColumn.ERR_MSG;
import static org.usf.inspect.server.config.TraceApiColumn.ERR_TYPE;
import static org.usf.inspect.server.config.TraceApiColumn.ID;
import static org.usf.inspect.server.config.TraceApiColumn.INSTANCE_ENV;
import static org.usf.inspect.server.config.TraceApiColumn.LOCATION;
import static org.usf.inspect.server.config.TraceApiColumn.MASK;
import static org.usf.inspect.server.config.TraceApiColumn.NAME;
import static org.usf.inspect.server.config.TraceApiColumn.STACKTRACE;
import static org.usf.inspect.server.config.TraceApiColumn.START;
import static org.usf.inspect.server.config.TraceApiColumn.THREAD;
import static org.usf.inspect.server.config.TraceApiColumn.TYPE;
import static org.usf.inspect.server.config.TraceApiColumn.USER;
import static org.usf.inspect.server.mapper.InspectMappers.createBaseMainSession;
import static org.usf.jquery.core.JDBCType.VARCHAR;
import static org.usf.jquery.core.Operators.function;
import static org.usf.jquery.core.Parameter.required;
import static org.usf.jquery.core.Parameter.varargs;
import static org.usf.jquery.core.QueryExecutor.defaultExecutor;
import static org.usf.jquery.core.TypeResolver.firstArgType;

import org.usf.inspect.core.InspectCollectorConfiguration;
import org.usf.inspect.core.InstanceEnvironment;
import org.usf.inspect.core.InstanceType;
import org.usf.inspect.core.MachineResource;
import org.usf.inspect.core.StackTraceRow;
import org.usf.inspect.server.model.MainSession;
import org.usf.jquery.core.OperatorDefinition;
import org.usf.jquery.core.ResultSetMapper;
import org.usf.jquery.mvc.Bind;
import org.usf.jquery.mvc.Expose;
import org.usf.jquery.mvc.StoreResource;
import org.usf.jquery.mvc.ViewRegistry;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public interface InspectStore extends StoreResource {

	
	static ViewRegistry registry = new ViewRegistry()
			.register("instanceMapper", rsp-> defaultExecutor(instanceEnvironmentMapper(defaultMapper)))
			.register("mainSessionMapper", rsp-> defaultExecutor(createBaseMainSession(defaultMapper)))
			;
	
	
	@Bind("e_rst_rqt")
	@Expose(identity = "rest_request")
	RestRequest restRequest();
	
	@Bind("e_rst_rqt_stg")
	RestRequestStage restRequestStage();
	
	@Bind("e_rst_ses")
	@Expose(identity = "rest_session")
	RestSession restSession();
	
	@Bind("e_rst_ses_stg")
	RestSessionStage restSessionStage();
	
	@Bind("e_main_ses")
	@Expose(identity = "main_session")
	MainSession mainSession();
	
	@Bind("e_dtb_rqt")
	@Expose(identity = "database_request")
	DBRequest dbRequest();
	
	@Bind("e_dtb_stg")
	DBStage dbStage();
	
	@Bind("e_ftp_rqt")
	@Expose(identity = "ftp_request")
	FTPRequest ftpRequest();
	
	@Bind("e_ftp_stg")
	FTPStage ftpStage();
	
	@Bind("e_smtp_rqt")
	@Expose(identity = "smtp_request")
	SMTPRequest smtpRequest();
	
	@Bind("e_smtp_stg")
	SMTPStage smtpStage();
	
	@Bind("e_smtp_mail")
	SMTPMail smtpMail();
	
	@Bind("e_ldap_rqt")
	@Expose(identity = "ldap_request")
	LDAPRequest ldapRequest();
	
	@Bind("e_ldap_stg")
	LDAPStage ldapStage();
	
	@Bind("e_lcl_rqt")
	LocalRequest localRequest();
	
	@Bind("e_exc_inf")
	Exception exception();
	
	@Bind("e_env_ins")
	Instance instance();
	
	@Bind("e_usr_acn")
	@Expose(identity = "user_action")
	UserAction userAction();
	
	@Bind("e_ins_trc")
	@Expose(identity = "instance_trace")
	InstanceTrace instanceTrace();
	
	@Bind("e_log_ent")
	@Expose(identity = "log_entry")
	LogEntry logEntry();
	
	@Bind("e_rsc_usg")
	@Expose(identity = "resource_usage")
	ResourceUsage resourceUsage();
	
	default OperatorDefinition coalesce() {
		return function(firstArgType(), "COALESCE", required(), varargs(VARCHAR));
	}
	
	@Override
	default ViewRegistry viewRegistry() {
		return registry;
	}
	

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
}
