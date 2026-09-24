package org.usf.inspect.server.erm;

import static org.usf.inspect.server.erm.ViewRegistryConstant.*;
import static org.usf.inspect.server.mapper.Mappers.*;
import static org.usf.jquery.core.JDBCType.VARCHAR;
import static org.usf.jquery.core.Mappers.toListMapper;
import static org.usf.jquery.core.Operators.function;
import static org.usf.jquery.core.Parameter.required;
import static org.usf.jquery.core.Parameter.varargs;
import static org.usf.jquery.core.Predicate.*;
import static org.usf.jquery.core.QueryExecutor.defaultExecutor;
import static org.usf.jquery.core.TypeResolver.firstArgType;

import org.usf.jquery.core.Chainable;
import org.usf.jquery.core.OperatorDefinition;
import org.usf.jquery.core.Predicate;
import org.usf.jquery.mvc.Bind;
import org.usf.jquery.mvc.Expose;
import org.usf.jquery.mvc.StoreCatalog;
import org.usf.jquery.mvc.ViewRegistry;

public interface InspectStore extends StoreCatalog {

	@Bind("e_rst_rqt")
	@Expose(identity = "rest_request")
	RestRequestCatalog restRequest();
	
	@Bind("e_rst_rqt_stg")
	@Expose(identity = "rest_request_stage")
	RestRequestStageCatalog restRequestStage();
	
	@Bind("e_rst_ses")
	@Expose(identity = "rest_session")
	RestSessionCatalog restSession();
	
	@Bind("e_rst_ses_stg")
	@Expose(identity = "rest_session_stage")
	RestSessionStageCatalog restSessionStage();
	
	@Bind("e_main_ses")
	@Expose(identity = "main_session")
	MainSessionCatalog mainSession();
	
	@Bind("e_dtb_rqt")
	@Expose(identity = "database_request")
	DatabaseRequestCatalog databaseRequest();
	
	@Bind("e_dtb_stg")
	@Expose(identity = "database_stage")
	DatabaseStageCatalog databaseRequestStage();
	
	@Bind("e_ftp_rqt")
	@Expose(identity = "ftp_request")
	FtpRequestCatalog ftpRequest();
	
	@Bind("e_ftp_stg")
	@Expose(identity = "ftp_stage")
	FtpStageCatalog ftpStage();
	
	@Bind("e_smtp_rqt")
	@Expose(identity = "smtp_request")
	SmtpRequestCatalog smtpRequest();
	
	@Bind("e_smtp_stg")
	@Expose(identity = "smtp_stage")
	SmtpStageCatalog smtpStage();
	
	@Bind("e_smtp_mail")
	@Expose(identity = "smtp_mail")
	SmtpMailCatalog smtpMail();
	
	@Bind("e_ldap_rqt")
	@Expose(identity = "ldap_request")
	LdapRequestCatalog ldapRequest();
	
	@Bind("e_ldap_stg")
	@Expose(identity = "ldap_stage")
	LdapStageCatalog ldapStage();
	
	@Bind("e_lcl_rqt")
	@Expose(identity = "local_request")
	LocalRequestCatalog localRequest();
	
	@Bind("e_exc_inf")
	ExceptionCatalog exception();
	
	@Bind("e_env_ins")
	InstanceCatalog instance();
	
	@Bind("e_usr_acn")
	@Expose(identity = "user_action")
	UserActionCatalog userAction();
	
	@Bind("e_ins_trc")
	@Expose(identity = "instance_trace")
	InstanceTraceCatalog instanceTrace();
	
	@Bind("e_log_ent")
	@Expose(identity = "log_entry")
	LogEntryCatalog logEntry();
	
	@Bind("e_rsc_usg")
	@Expose(identity = "resource_usage")
	ResourceUsageCatalog resourceUsage();
	
	default OperatorDefinition coalesce() {
		return function(firstArgType(), "COALESCE", required(), varargs(VARCHAR));
	}

	default Predicate origin(String... values){
		return Chainable.or(values, v-> switch(v){
			case "5xx"-> ge(500);
			case "4xx"-> ge(400).and(lt(500));
			case "2xx"-> ge(200).and(lt(300));
			case "0"->	 eq(0);
			case "pending"-> isNull();
			default -> null;
		});
	}

	@Override
	default ViewRegistry viewRegistry() {
		return registry;
	}
	
	static ViewRegistry registry = new ViewRegistry()
			.register(INSTANCE_ENVIRONMENT_RESULTSET_MAPPER, rsp-> defaultExecutor(instanceEnvironmentResultSetMapper()))
			.register(LOG_ENTRY_ROW_MAPPER, rsp-> defaultExecutor(toListMapper(logEntryRowMapper()) ))
			.register(INSTANCE_TRACE_ROW_MAPPER, rsp-> defaultExecutor(toListMapper(instanceTraceRowMapper()) ))
			.register(MACHINE_RESOURCE_USAGE_ROW_MAPPER, rsp-> defaultExecutor(toListMapper(machineResourceUsageRowMapper()) ))
			.register(REST_SESSION_ROW_MAPPER, rsp-> defaultExecutor(toListMapper(restSessionRowMapper())))
			.register(REST_SESSION_RESULTSET_MAPPER, rsp-> defaultExecutor(restSessionResultSetMapper()))
			.register(REST_SESSION_STAGE_ROW_MAPPER, rsp-> defaultExecutor(toListMapper(restSessionStageRowMapper())))
			.register(REST_SESSION_PULSE_ROW_MAPPER, rsp-> defaultExecutor(toListMapper(restSessionPulseRowMapper())))
			.register(MAIN_SESSION_ROW_MAPPER, rsp-> defaultExecutor(toListMapper(mainSessionRowMapper())))
			.register(MAIN_SESSION_RESULTSET_MAPPER, rsp-> defaultExecutor(mainSessionResultSetMapper()))
			.register(MAIN_SESSION_PULSE_ROW_MAPPER, rsp-> defaultExecutor(toListMapper(mainSessionPulseRowMapper())))
			.register(REST_REQUEST_ROW_MAPPER, rsp-> defaultExecutor(toListMapper(restRequestRowMapper())))
			.register(REST_REQUEST_RESULTSET_MAPPER, rsp-> defaultExecutor(restRequestResultSetMapper()))
			.register(REST_REQUEST_STAGE_ROW_MAPPER, rsp-> defaultExecutor(toListMapper(restRequestStageRowMapper())))
			.register(LOCAL_REQUEST_ROW_MAPPER, rsp-> defaultExecutor(toListMapper(localRequestRowMapper())))
			.register(DATABASE_REQUEST_ROW_MAPPER, rsp-> defaultExecutor(toListMapper(databaseRequestRowMapper())))
			.register(DATABASE_REQUEST_RESULTSET_MAPPER, rsp-> defaultExecutor(databaseRequestResultSetMapper()))
			.register(DATABASE_REQUEST_STAGE_ROW_MAPPER, rsp-> defaultExecutor(toListMapper(databaseRequestStageRowMapper())))
			.register(FTP_REQUEST_ROW_MAPPER, rsp-> defaultExecutor(toListMapper(ftpRequestRowMapper())))
			.register(FTP_REQUEST_RESULTSET_MAPPER, rsp-> defaultExecutor(ftpRequestResultSetMapper()))
			.register(FTP_REQUEST_STAGE_ROW_MAPPER, rsp-> defaultExecutor(toListMapper(ftpRequestStageRowMapper())))
			.register(SMTP_REQUEST_ROW_MAPPER, rsp-> defaultExecutor(toListMapper(smtpRequestRowMapper())))
			.register(SMTP_REQUEST_RESULTSET_MAPPER, rsp-> defaultExecutor(smtpRequestResultSetMapper()))
			.register(SMTP_REQUEST_STAGE_ROW_MAPPER, rsp-> defaultExecutor(toListMapper(smtpRequestStageRowMapper())))
			.register(SMTP_REQUEST_MAIL_ROW_MAPPER, rsp-> defaultExecutor(toListMapper(smtpRequestMailRowMapper())))
			.register(LDAP_REQUEST_ROW_MAPPER, rsp-> defaultExecutor(toListMapper(ldapRequestRowMapper())))
			.register(LDAP_REQUEST_RESULTSET_MAPPER, rsp-> defaultExecutor(ldapRequestResultSetMapper()))
			.register(LDAP_REQUEST_STAGE_ROW_MAPPER, rsp-> defaultExecutor(toListMapper(ldapRequestStageRowMapper())))
			.register(EXCEPTION_BY_REQUEST_RESULTSET_MAPPER, rsp-> defaultExecutor(exceptionByRequestResultSetMapper()))
			.register(USER_ACTION_ROW_MAPPER, rsp-> defaultExecutor(toListMapper(userActionRowMapper())));
	
}
