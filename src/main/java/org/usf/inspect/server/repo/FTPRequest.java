package org.usf.inspect.server.repo;

import static org.usf.inspect.server.config.constant.FieldConstant.CD_INS;
import static org.usf.inspect.server.config.constant.FieldConstant.CD_PRN_SES;
import static org.usf.inspect.server.config.constant.FieldConstant.CD_PRT;
import static org.usf.inspect.server.config.constant.FieldConstant.DH_END;
import static org.usf.inspect.server.config.constant.FieldConstant.DH_STR;
import static org.usf.inspect.server.config.constant.FieldConstant.ID_FTP_RQT;
import static org.usf.inspect.server.config.constant.FieldConstant.VA_CLT_VRS;
import static org.usf.inspect.server.config.constant.FieldConstant.VA_CMD;
import static org.usf.inspect.server.config.constant.FieldConstant.VA_FAIL;
import static org.usf.inspect.server.config.constant.FieldConstant.VA_HST;
import static org.usf.inspect.server.config.constant.FieldConstant.VA_PCL;
import static org.usf.inspect.server.config.constant.FieldConstant.VA_SRV_VRS;
import static org.usf.inspect.server.config.constant.FieldConstant.VA_THR;
import static org.usf.inspect.server.config.constant.FieldConstant.VA_USR;

import org.usf.jquery.core.Column;
import org.usf.jquery.core.ViewColumn;
import org.usf.jquery.web.proxy.Bind;
import org.usf.jquery.web.proxy.DatasetResource;
import org.usf.jquery.web.proxy.Expose;

public interface FTPRequest extends DatasetResource {

	@Bind(ID_FTP_RQT)
	ViewColumn id();
	
	@Bind(VA_HST)
	ViewColumn host();
	
	@Bind(CD_PRT)
	ViewColumn port();
	
	@Bind(VA_PCL)
	ViewColumn protocol();
	
	@Bind(VA_SRV_VRS)
	ViewColumn serverVersion();
	
	@Bind(VA_CLT_VRS)
	ViewColumn clientVersion();
	
	@Bind(VA_USR)
	ViewColumn user();
	
	@Bind(DH_STR)
	ViewColumn start();
	
	@Bind(DH_END)
	ViewColumn end();
	
	@Bind(VA_THR)
	ViewColumn thread();
	
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
