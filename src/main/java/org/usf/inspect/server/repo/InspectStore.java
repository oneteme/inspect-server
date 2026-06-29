package org.usf.inspect.server.repo;

import org.usf.jquery.mvc.Bind;
import org.usf.jquery.mvc.Expose;
import org.usf.jquery.mvc.StoreResource;

public interface InspectStore extends StoreResource {

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
	
}
