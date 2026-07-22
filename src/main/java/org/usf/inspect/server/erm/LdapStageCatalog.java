package org.usf.inspect.server.erm;

import static org.usf.inspect.core.RequestMask.LDAP;
import static org.usf.inspect.server.config.constant.FieldConstant.CD_LDAP_RQT;
import static org.usf.inspect.server.config.constant.FieldConstant.CD_ORD;
import static org.usf.inspect.server.config.constant.FieldConstant.DH_END;
import static org.usf.inspect.server.config.constant.FieldConstant.DH_STR;
import static org.usf.inspect.server.config.constant.FieldConstant.VA_ARG;
import static org.usf.inspect.server.config.constant.FieldConstant.VA_CMD;
import static org.usf.inspect.server.config.constant.FieldConstant.VA_NAM;
import static org.usf.jquery.core.JDBCType.UUID;

import org.usf.inspect.core.RequestMask;
import org.usf.jquery.core.Column;
import org.usf.jquery.core.ViewColumn;
import org.usf.jquery.mvc.Bind;
import org.usf.jquery.mvc.DatasetCatalog;
import org.usf.jquery.mvc.Typed;

public interface LdapStageCatalog extends StageCatalog {
	
	@Bind(DH_STR)
	ViewColumn start();
	
	@Bind(DH_END)
	ViewColumn end();
	
	@Bind(VA_ARG)
	ViewColumn arg();
	
	@Bind(VA_CMD)
	ViewColumn command();
	
	@Bind(CD_LDAP_RQT)
	@Typed(UUID)
	ViewColumn parent();

	@Override
	default RequestMask getRequestType() {
		return LDAP;
	}

	default Column elapsedTime() {
		return end().minus(start()).epoch();
	}
}
