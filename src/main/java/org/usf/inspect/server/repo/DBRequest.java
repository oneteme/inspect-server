package org.usf.inspect.server.repo;

import static org.usf.inspect.server.config.constant.FieldConstant.CD_INS;
import static org.usf.inspect.server.config.constant.FieldConstant.CD_PRN_SES;
import static org.usf.inspect.server.config.constant.FieldConstant.CD_PRT;
import static org.usf.inspect.server.config.constant.FieldConstant.DH_END;
import static org.usf.inspect.server.config.constant.FieldConstant.DH_STR;
import static org.usf.inspect.server.config.constant.FieldConstant.ID_DTB_RQT;
import static org.usf.inspect.server.config.constant.FieldConstant.VA_CMD;
import static org.usf.inspect.server.config.constant.FieldConstant.VA_DRV;
import static org.usf.inspect.server.config.constant.FieldConstant.VA_FAIL;
import static org.usf.inspect.server.config.constant.FieldConstant.VA_HST;
import static org.usf.inspect.server.config.constant.FieldConstant.VA_NAM;
import static org.usf.inspect.server.config.constant.FieldConstant.VA_PRD_NAM;
import static org.usf.inspect.server.config.constant.FieldConstant.VA_PRD_VRS;
import static org.usf.inspect.server.config.constant.FieldConstant.VA_SHA;
import static org.usf.inspect.server.config.constant.FieldConstant.VA_THR;
import static org.usf.inspect.server.config.constant.FieldConstant.VA_USR;

import org.usf.jquery.core.Column;
import org.usf.jquery.core.ViewColumn;
import org.usf.jquery.mvc.Bind;
import org.usf.jquery.mvc.DatasetResource;
import org.usf.jquery.mvc.Expose;

public interface DBRequest extends DatasetResource {

	@Bind(ID_DTB_RQT)
	ViewColumn id();
	
	@Bind(VA_HST)
	ViewColumn host();
	
	@Bind(CD_PRT)
	ViewColumn port();
	
	@Bind(VA_NAM)
	ViewColumn db();		
	
	@Bind(VA_SHA)
	ViewColumn schema();
	
	@Bind(DH_STR)
	ViewColumn start();
	
	@Bind(DH_END)
	ViewColumn end();	
	
	@Bind(VA_USR)
	ViewColumn user();
	
	@Bind(VA_THR)
	ViewColumn thread();
	
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
	
	@Bind(CD_INS)
	@Expose(identity = "instance_env")
	ViewColumn instanceEnv();
	
	default Column elapsedTime() {
		return end().minus(start()).epoch();
	}
}
