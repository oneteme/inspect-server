package org.usf.inspect.server.erm;

import static org.usf.inspect.core.RequestMask.LDAP;
import static org.usf.inspect.server.config.constant.FieldConstant.CD_PRN_SES;
import static org.usf.inspect.server.config.constant.FieldConstant.ID_LDAP_RQT;
import static org.usf.inspect.server.config.constant.FieldConstant.VA_CMD;
import static org.usf.inspect.server.config.constant.FieldConstant.VA_FAIL;
import static org.usf.inspect.server.config.constant.FieldConstant.VA_PCL;

import org.usf.inspect.core.RequestMask;
import org.usf.jquery.core.ViewColumn;
import org.usf.jquery.mvc.Bind;

public interface LdapRequestCatalog extends RequestCatalog {

	@Bind(ID_LDAP_RQT)
	ViewColumn id();
	
	@Bind(VA_PCL)
	ViewColumn protocol();
	
	@Bind(VA_CMD)
	ViewColumn command();
	
	@Bind(VA_FAIL)
	ViewColumn failed();
	
	@Bind(CD_PRN_SES)
	ViewColumn parent();
	
	@Override
	default RequestMask getRequestType() {
		return LDAP;
	}
}
