package org.usf.inspect.server.erm;

import org.usf.jquery.core.ViewColumn;
import org.usf.jquery.mvc.Bind;
import org.usf.jquery.mvc.Typed;

import static org.usf.inspect.server.config.constant.FieldConstant.CD_SMTP_RQT;
import static org.usf.inspect.server.config.constant.FieldConstant.VA_CMD;
import static org.usf.jquery.core.JDBCType.UUID;

public interface SmtpStageCatalog extends StageCatalog {
	
	@Bind(VA_CMD)
	ViewColumn command();
	
	@Bind(CD_SMTP_RQT)
	@Typed(UUID)
	ViewColumn parent();
}
