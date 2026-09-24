package org.usf.inspect.server.erm;

import org.usf.jquery.core.Column;
import org.usf.jquery.core.ViewColumn;
import org.usf.jquery.mvc.Bind;
import org.usf.jquery.mvc.Expose;
import org.usf.jquery.mvc.Typed;

import static org.usf.inspect.server.config.constant.FieldConstant.*;
import static org.usf.jquery.core.JDBCType.UUID;

public interface DatabaseStageCatalog extends StageCatalog {
	
	@Bind(DH_STR)
	ViewColumn start();
	
	@Bind(DH_END)
	ViewColumn end();
	
	@Bind(VA_CMD)
	ViewColumn command();
	
	@Bind(CD_DTB_RQT)
	@Typed(UUID)
	ViewColumn parent();
	
	default Column elapsedTime() {
		return end().minus(start()).epoch();
	}
}
