package org.usf.inspect.server.erm;

import org.usf.inspect.core.RequestMask;
import org.usf.jquery.core.Column;
import org.usf.jquery.core.ViewColumn;
import org.usf.jquery.mvc.Bind;
import org.usf.jquery.mvc.Typed;

import static org.usf.inspect.core.RequestMask.REST;
import static org.usf.inspect.server.config.constant.FieldConstant.*;
import static org.usf.jquery.core.JDBCType.UUID;

public interface RestRequestStageCatalog extends StageCatalog {
	
	@Bind(DH_STR)
	ViewColumn start();
	
	@Bind(DH_END)
	ViewColumn end();
	
	@Bind(CD_RST_RQT)
	@Typed(UUID)
	ViewColumn parent();

	@Override
	default RequestMask getRequestType() {
		return REST;
	}

	default Column elapsedTime() {
		return end().minus(start()).epoch();
	}
}
