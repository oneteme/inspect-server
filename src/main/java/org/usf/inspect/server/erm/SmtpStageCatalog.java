package org.usf.inspect.server.erm;

import static org.usf.inspect.core.SessionMask.SMTP;
import static org.usf.inspect.server.config.constant.FieldConstant.CD_ORD;
import static org.usf.inspect.server.config.constant.FieldConstant.CD_SMTP_RQT;
import static org.usf.inspect.server.config.constant.FieldConstant.DH_END;
import static org.usf.inspect.server.config.constant.FieldConstant.DH_STR;
import static org.usf.inspect.server.config.constant.FieldConstant.VA_CMD;
import static org.usf.inspect.server.config.constant.FieldConstant.VA_NAM;
import static org.usf.jquery.core.JDBCType.UUID;

import org.usf.inspect.core.SessionMask;
import org.usf.jquery.core.Column;
import org.usf.jquery.core.ViewColumn;
import org.usf.jquery.mvc.Bind;
import org.usf.jquery.mvc.DatasetCatalog;
import org.usf.jquery.mvc.Typed;

public interface SmtpStageCatalog extends StageCatalog {
	
	@Bind(DH_STR)
	ViewColumn start();
	
	@Bind(DH_END)
	ViewColumn end();
	
	@Bind(VA_CMD)
	ViewColumn command();
	
	@Bind(CD_SMTP_RQT)
	@Typed(UUID)
	ViewColumn parent();

	@Override
	default SessionMask getRequestType() {
		return SMTP;
	}

	default Column elapsedTime() {
		return end().minus(start()).epoch();
	}
}
