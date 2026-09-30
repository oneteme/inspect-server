package org.usf.inspect.server.erm;

import org.usf.jquery.core.Column;
import org.usf.jquery.core.ViewColumn;
import org.usf.jquery.mvc.Bind;
import org.usf.jquery.mvc.Expose;
import org.usf.jquery.mvc.Typed;

import static org.usf.inspect.server.config.constant.FieldConstant.*;
import static org.usf.jquery.core.JDBCType.UUID;
import static org.usf.jquery.core.Predicate.ge;
import static org.usf.jquery.core.Predicate.lt;

public interface DatabaseRequestCatalog extends RequestCatalog {

	@Bind(ID_DTB_RQT)
	@Typed(UUID)
	ViewColumn id();
	
	@Bind(VA_NAM)
	ViewColumn db();

	@Bind(VA_SHE)
	ViewColumn scheme();

	@Bind(VA_SHA)
	ViewColumn schema();
	
	@Bind(VA_DRV)
	ViewColumn driver();
	
	@Bind(VA_PRD_NAM)
	@Expose(identity = "db_name")
	ViewColumn dbName();
	
	@Bind(VA_PRD_VRS)
	@Expose(identity = "db_version")
	ViewColumn dbVersion();
	
	@Bind(VA_CMD)
	ViewColumn command();
}
