package org.usf.inspect.server.erm;

import org.usf.jquery.core.ViewColumn;
import org.usf.jquery.mvc.Bind;
import org.usf.jquery.mvc.Typed;

import static org.usf.inspect.server.config.constant.FieldConstant.ID_SMTP_RQT;
import static org.usf.inspect.server.config.constant.FieldConstant.VA_CMD;
import static org.usf.jquery.core.JDBCType.UUID;

public interface SmtpRequestCatalog extends RequestCatalog {

	@Bind(ID_SMTP_RQT)
	@Typed(UUID)
	ViewColumn id();
	
	@Bind(VA_CMD)
	ViewColumn command();
}
