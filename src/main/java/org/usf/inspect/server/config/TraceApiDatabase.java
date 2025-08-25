package org.usf.inspect.server.config;

import org.usf.jquery.web.DatabaseDecorator;
import org.usf.jquery.web.ViewDecorator;

public enum TraceApiDatabase implements DatabaseDecorator {
	
	INSPECT;

	@Override
	public String identity() {
		return "inspect";
	}

	@Override
	public String viewName(ViewDecorator vd) {
		return switch ((TraceApiTable) vd) {
			case REST_REQUEST: yield "e_rst_rqt";
			case REST_REQUEST_STAGE: yield "e_rst_rqt_stg";
			case REST_SESSION: yield "e_rst_ses";
			case REST_SESSION_STAGE: yield "e_rst_ses_stg";
			case MAIN_SESSION: yield "e_main_ses";
			case DATABASE_REQUEST: yield "e_dtb_rqt";
			case DATABASE_STAGE: yield "e_dtb_stg";
			case FTP_REQUEST: yield "e_ftp_rqt";
			case FTP_STAGE: yield "e_ftp_stg";
			case SMTP_REQUEST: yield "e_smtp_rqt";
			case SMTP_STAGE: yield "e_smtp_stg";
			case SMTP_MAIL: yield "e_smtp_mail";
			case LDAP_REQUEST: yield "e_ldap_rqt";
			case LDAP_STAGE: yield "e_ldap_stg";
			case LOCAL_REQUEST: yield "e_lcl_rqt";
			case EXCEPTION: yield "e_exc_inf";
			case INSTANCE: yield "e_env_ins";
			case USER_ACTION: yield "e_usr_acn";
			case INSTANCE_TRACE: yield "e_ins_trc";
			case LOG_ENTRY: yield "e_log_ent";
			case RESOURCE_USAGE: yield "e_rsc_usg";
			default: yield null;
		};
	}
}
