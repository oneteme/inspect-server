package org.usf.inspect.server.erm;

import static org.usf.inspect.core.RequestMask.JDBC;
import static org.usf.inspect.server.config.constant.FieldConstant.CD_PRN_SES;
import static org.usf.inspect.server.config.constant.FieldConstant.ID_DTB_RQT;
import static org.usf.inspect.server.config.constant.FieldConstant.VA_CMD;
import static org.usf.inspect.server.config.constant.FieldConstant.VA_DRV;
import static org.usf.inspect.server.config.constant.FieldConstant.VA_FAIL;
import static org.usf.inspect.server.config.constant.FieldConstant.VA_NAM;
import static org.usf.inspect.server.config.constant.FieldConstant.VA_PRD_NAM;
import static org.usf.inspect.server.config.constant.FieldConstant.VA_PRD_VRS;
import static org.usf.inspect.server.config.constant.FieldConstant.VA_SHA;

import org.usf.inspect.core.RequestMask;
import org.usf.jquery.core.ViewColumn;
import org.usf.jquery.mvc.Bind;

public interface DatabaseRequestCatalog extends RequestCatalog {

	@Bind(ID_DTB_RQT)
	ViewColumn id();
	
	@Bind(VA_NAM)
	ViewColumn db();		
	
	@Bind(VA_SHA)
	ViewColumn schema();
	
	@Bind(VA_DRV)
	ViewColumn driver();
	
	@Bind(VA_PRD_NAM)
	ViewColumn dbName();
	
	@Bind(VA_PRD_VRS)
	ViewColumn dbVersion();
	
	@Bind(VA_CMD)
	ViewColumn command();
	
	@Bind(VA_FAIL)
	ViewColumn failed();
	
	@Bind(CD_PRN_SES)
	ViewColumn parent();
	
	@Override
	default RequestMask getRequestType() {
		return JDBC;
	}
}
