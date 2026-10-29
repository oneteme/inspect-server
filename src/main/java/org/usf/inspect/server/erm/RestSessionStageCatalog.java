package org.usf.inspect.server.erm;

import org.usf.jquery.core.ViewColumn;
import org.usf.jquery.mvc.Bind;
import org.usf.jquery.mvc.Typed;

import static org.usf.inspect.server.config.constant.FieldConstant.CD_PRN_SES;
import static org.usf.jquery.core.JDBCType.UUID;

public interface RestSessionStageCatalog extends StageCatalog {
	
	@Bind(CD_PRN_SES)
	@Typed(UUID)
	ViewColumn parent();
}
