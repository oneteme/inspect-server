package org.usf.inspect.server.repo;

import static org.usf.inspect.server.config.constant.FieldConstant.CD_ORD;
import static org.usf.inspect.server.config.constant.FieldConstant.CD_SMTP_RQT;
import static org.usf.inspect.server.config.constant.FieldConstant.DH_END;
import static org.usf.inspect.server.config.constant.FieldConstant.DH_STR;
import static org.usf.inspect.server.config.constant.FieldConstant.VA_CMD;
import static org.usf.inspect.server.config.constant.FieldConstant.VA_NAM;

import org.usf.jquery.core.Column;
import org.usf.jquery.core.ViewColumn;
import org.usf.jquery.web.proxy.Bind;
import org.usf.jquery.web.proxy.DatasetResource;

public interface SMTPStage extends DatasetResource {
	
	@Bind(VA_NAM)
	ViewColumn name();
	
	@Bind(DH_STR)
	ViewColumn start();
	
	@Bind(DH_END)
	ViewColumn end();
	
	@Bind(VA_CMD)
	ViewColumn command();
	
	@Bind(CD_ORD)
	ViewColumn order();
	
	@Bind(CD_SMTP_RQT)
	ViewColumn parent();
	
	default Column elapsedTime() {
		return end().minus(start()).epoch();
	}
}
