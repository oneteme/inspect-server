package org.usf.inspect.server.erm;

import org.usf.jquery.core.ViewColumn;
import org.usf.jquery.mvc.Bind;
import org.usf.jquery.mvc.Typed;

import static org.usf.inspect.server.config.constant.FieldConstant.CD_RST_RQT;
import static org.usf.jquery.core.JDBCType.UUID;

public interface RestRequestStageCatalog extends StageCatalog {
	@Bind(CD_RST_RQT)
	@Typed(UUID)
	ViewColumn parent();
}
