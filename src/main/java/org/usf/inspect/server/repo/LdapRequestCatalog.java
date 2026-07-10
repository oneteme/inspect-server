package org.usf.inspect.server.repo;

import org.usf.jquery.core.Column;
import org.usf.jquery.core.JoinGroup;
import org.usf.jquery.core.ViewColumn;
import org.usf.jquery.mvc.Bind;

import static org.usf.inspect.core.RequestMask.LDAP;
import static org.usf.inspect.server.config.constant.FieldConstant.*;
import static org.usf.jquery.core.Join.leftJoin;
import static org.usf.jquery.core.JoinGroup.joins;
import static org.usf.jquery.mvc.StoreManager.getInstance;

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
	
	default Column elapsedTime() {
		return end().minus(start()).epoch();
	}
	
	default JoinGroup exception() {
		var exception = getInstance().getStore(InspectStore.class).exception();
		return joins(leftJoin(exception.getView(), exception.parent().eq(id()), exception.type().eq(LDAP.name())));
	}
}
