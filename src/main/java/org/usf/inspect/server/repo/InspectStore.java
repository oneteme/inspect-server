package org.usf.inspect.server.repo;

import org.usf.jquery.web.proxy.Bind;
import org.usf.jquery.web.proxy.Expose;
import org.usf.jquery.web.proxy.StoreResource;

public interface InspectStore extends StoreResource {

	@Bind("e_rst_rqt")
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
	DBRequest dbRequest();
	
	@Bind("e_dtb_stg")
	DBStage dbStage();
	
	@Bind("e_ftp_rqt")
	FTPRequest ftpRequest();
	
	@Bind("e_ftp_stg")
	FTPStage ftpStage();
	
	@Bind("e_smtp_rqt")
	SMTPRequest smtpRequest();
	
	@Bind("e_smtp_stg")
	SMTPStage smtpStage();
	
	@Bind("e_smtp_mail")
	SMTPMail smtpMail();
	
	@Bind("e_ldap_rqt")
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
	UserAction userAction();
	
	@Bind("e_ins_trc")
	InstanceTrace instanceTrace();
	
	@Bind("e_log_ent")
	LogEntry logEntry();
	
	@Bind("e_rsc_usg")
	ResourceUsage resourceUsage();
	
}
