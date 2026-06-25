package org.usf.inspect.server.repo;

import org.usf.jquery.core.ViewColumn;
import org.usf.jquery.web.proxy.Bind;
import org.usf.jquery.web.proxy.DatasetResource;
import static org.usf.inspect.server.config.constant.FieldConstant.*;

public interface Exception extends DatasetResource {
	
	@Bind(VA_TYP)
	ViewColumn type();
	
	@Bind(VA_ERR_TYP)
	ViewColumn errType();
	
	@Bind(VA_ERR_MSG)
	ViewColumn errMsg();
	
	@Bind(VA_STK)
	ViewColumn stacktrace();
	
	@Bind(CD_ORD)
	ViewColumn order();
	
	@Bind(CD_RQT)
	ViewColumn parent();

}
