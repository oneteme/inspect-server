package org.usf.inspect.server.erm;

import static org.usf.inspect.core.SessionMask.FTP;
import static org.usf.inspect.server.config.constant.FieldConstant.CD_FTP_RQT;
import static org.usf.inspect.server.config.constant.FieldConstant.CD_ORD;
import static org.usf.inspect.server.config.constant.FieldConstant.DH_END;
import static org.usf.inspect.server.config.constant.FieldConstant.DH_STR;
import static org.usf.inspect.server.config.constant.FieldConstant.VA_ARG;
import static org.usf.inspect.server.config.constant.FieldConstant.VA_CMD;
import static org.usf.inspect.server.config.constant.FieldConstant.VA_NAM;
import static org.usf.jquery.core.JDBCType.UUID;

import org.usf.inspect.core.SessionMask;
import org.usf.jquery.core.Column;
import org.usf.jquery.core.ViewColumn;
import org.usf.jquery.mvc.Bind;
import org.usf.jquery.mvc.DatasetCatalog;
import org.usf.jquery.mvc.Typed;

public interface FtpStageCatalog extends StageCatalog {
	
	@Bind(DH_STR)
	ViewColumn start();
	
	@Bind(DH_END)
	ViewColumn end();
	
	@Bind(VA_ARG)
	ViewColumn arg();
	
	@Bind(VA_CMD)
	ViewColumn command();
	
	@Bind(CD_FTP_RQT)
	@Typed(UUID)
	ViewColumn parent();
	
	default Column elapsedTime() {
		return end().minus(start()).epoch();
	}

	@Override
	default SessionMask getRequestType() {
		return FTP;
	}
}
