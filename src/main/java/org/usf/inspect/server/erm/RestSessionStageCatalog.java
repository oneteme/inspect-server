package org.usf.inspect.server.erm;

import static org.usf.inspect.core.SessionMask.REST;
import static org.usf.inspect.server.config.constant.FieldConstant.CD_ORD;
import static org.usf.inspect.server.config.constant.FieldConstant.CD_PRN_SES;
import static org.usf.inspect.server.config.constant.FieldConstant.DH_END;
import static org.usf.inspect.server.config.constant.FieldConstant.DH_STR;
import static org.usf.inspect.server.config.constant.FieldConstant.VA_NAM;
import static org.usf.jquery.core.JDBCType.UUID;

import org.usf.inspect.core.SessionMask;
import org.usf.jquery.core.Column;
import org.usf.jquery.core.ViewColumn;
import org.usf.jquery.mvc.Bind;
import org.usf.jquery.mvc.DatasetCatalog;
import org.usf.jquery.mvc.Typed;

public interface RestSessionStageCatalog extends StageCatalog {
	
	@Bind(DH_STR)
	ViewColumn start();
	
	@Bind(DH_END)
	ViewColumn end();
	
	@Bind(CD_PRN_SES)
	@Typed(UUID)
	ViewColumn parent();

	@Override
	default SessionMask getRequestType() {
		throw new UnsupportedOperationException("REST session stage catalog does not support getRequestType()");
	}

	default Column elapsedTime() {
		return end().minus(start()).epoch();
	}
}
