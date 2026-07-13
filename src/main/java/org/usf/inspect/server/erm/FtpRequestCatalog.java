package org.usf.inspect.server.erm;

import static org.usf.inspect.core.RequestMask.FTP;
import static org.usf.inspect.server.config.constant.FieldConstant.CD_PRN_SES;
import static org.usf.inspect.server.config.constant.FieldConstant.ID_FTP_RQT;
import static org.usf.inspect.server.config.constant.FieldConstant.VA_CLT_VRS;
import static org.usf.inspect.server.config.constant.FieldConstant.VA_CMD;
import static org.usf.inspect.server.config.constant.FieldConstant.VA_FAIL;
import static org.usf.inspect.server.config.constant.FieldConstant.VA_PCL;
import static org.usf.inspect.server.config.constant.FieldConstant.VA_SRV_VRS;
import static org.usf.jquery.core.JDBCType.UUID;

import org.usf.inspect.core.RequestMask;
import org.usf.jquery.core.ViewColumn;
import org.usf.jquery.mvc.Bind;
import org.usf.jquery.mvc.Expose;
import org.usf.jquery.mvc.Typed;

public interface FtpRequestCatalog extends RequestCatalog {

	@Bind(ID_FTP_RQT)
	@Typed(UUID)
	ViewColumn id();
	
	@Bind(VA_PCL)
	ViewColumn protocol();
	
	@Bind(VA_SRV_VRS)
	@Expose(identity = "server_version")
	ViewColumn serverVersion();
	
	@Bind(VA_CLT_VRS)
	@Expose(identity = "client_version")
	ViewColumn clientVersion();
	
	@Bind(VA_CMD)
	ViewColumn command();
	
	@Bind(VA_FAIL)
	ViewColumn failed();
	
	@Bind(CD_PRN_SES)
	@Typed(UUID)
	ViewColumn parent();
	
	@Override
	default RequestMask getRequestType() {
		return FTP;
	}
}
