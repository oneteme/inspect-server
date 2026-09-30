package org.usf.inspect.server.erm;

import org.usf.jquery.core.ViewColumn;
import org.usf.jquery.mvc.Bind;
import org.usf.jquery.mvc.Expose;
import org.usf.jquery.mvc.Typed;

import static org.usf.inspect.server.config.constant.FieldConstant.*;
import static org.usf.jquery.core.JDBCType.UUID;

public interface FtpRequestCatalog extends RequestCatalog {

	@Bind(ID_FTP_RQT)
	@Typed(UUID)
	ViewColumn id();
	
	@Bind(VA_PCL)
	ViewColumn protocol();
	
	@Bind(VA_SRV_VRS)
	@Expose(identity = "server_version")
	ViewColumn serverVersion();
	
	@Bind(VA_CLT_VRS)
	@Expose(identity = "client_version")
	ViewColumn clientVersion();
	
	@Bind(VA_CMD)
	ViewColumn command();
}
