package org.usf.inspect.server.repo;

import static org.usf.inspect.server.config.constant.FieldConstant.CD_FTP_RQT;
import static org.usf.inspect.server.config.constant.FieldConstant.CD_ORD;
import static org.usf.inspect.server.config.constant.FieldConstant.DH_END;
import static org.usf.inspect.server.config.constant.FieldConstant.DH_STR;
import static org.usf.inspect.server.config.constant.FieldConstant.VA_ARG;
import static org.usf.inspect.server.config.constant.FieldConstant.VA_CMD;
import static org.usf.inspect.server.config.constant.FieldConstant.VA_NAM;

import org.usf.jquery.core.Column;
import org.usf.jquery.core.ViewColumn;
import org.usf.jquery.web.proxy.Bind;
import org.usf.jquery.web.proxy.DatasetResource;

public interface FTPStage extends DatasetResource {

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
	
	@Bind(CD_FTP_RQT)
	ViewColumn parent();
	
	default Column elapsedTime() {
		return end().minus(start()).epoch();
	}
}
