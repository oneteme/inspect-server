package org.usf.inspect.server.repo;

import org.usf.jquery.core.Column;
import org.usf.jquery.core.ViewColumn;
import org.usf.jquery.web.proxy.Bind;
import org.usf.jquery.web.proxy.DatasetResource;
import static org.usf.inspect.server.config.constant.FieldConstant.*;

public interface LDAPStage extends DatasetResource {
	
	@Bind(VA_NAM)
	ViewColumn name();
	
	@Bind(DH_STR)
	ViewColumn start();
	
	@Bind(DH_END)
	ViewColumn end();
	
	@Bind(VA_ARG)
	ViewColumn arg();
	
	@Bind(VA_CMD)
	ViewColumn command();
	
	@Bind(CD_ORD)
	ViewColumn order();
	
	@Bind(CD_LDAP_RQT)
	ViewColumn parent();

	
	default Column elapsedTime() {
		return end().minus(start()).epoch();
	}
}
