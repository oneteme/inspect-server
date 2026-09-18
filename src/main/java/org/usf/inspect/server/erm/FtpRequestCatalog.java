package org.usf.inspect.server.erm;

import static org.usf.inspect.core.SessionMask.FTP;
import static org.usf.inspect.server.config.constant.FieldConstant.CD_PRN_SES;
import static org.usf.inspect.server.config.constant.FieldConstant.ID_FTP_RQT;
import static org.usf.inspect.server.config.constant.FieldConstant.VA_CLT_VRS;
import static org.usf.inspect.server.config.constant.FieldConstant.VA_CMD;
import static org.usf.inspect.server.config.constant.FieldConstant.VA_FAIL;
import static org.usf.inspect.server.config.constant.FieldConstant.VA_PCL;
import static org.usf.inspect.server.config.constant.FieldConstant.VA_SRV_VRS;
import static org.usf.jquery.core.JDBCType.UUID;
import static org.usf.jquery.core.Predicate.*;

import org.usf.inspect.core.SessionMask;
import org.usf.jquery.core.Column;
import org.usf.jquery.core.ViewColumn;
import org.usf.jquery.mvc.Bind;
import org.usf.jquery.mvc.Expose;
import org.usf.jquery.mvc.Typed;

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
	
	@Bind(VA_FAIL)
	ViewColumn failed();
	
	@Bind(CD_PRN_SES)
	@Typed(UUID)
	ViewColumn parent();
	
	@Override
	default SessionMask getRequestType() {
		return FTP;
	}

	@Expose(identity = "count_request_error")
	default Column countError() {
		return failed().toCase().when(eq(true), failed()).compose().count();
	}

	default Column status() {
		return Column.beginCase()
				.when(end().isNull(), null)
				.when(failed().eq(true), 500)
				.when(failed().eq(false), 200)
				.compose().as("status");
	}

	@Expose(identity = "performance_tranche")
	default Column performanceTranche1() {
		return elapsedTime().toCase()
				.when(lt(1), "1")
				.when(ge(1).and(lt(3)), "2")
				.when(ge(3).and(lt(5)), "3")
				.when(ge(5).and(lt(10)), "4")
				.when(ge(10), "5").compose();
	}

	@Expose(identity = "performance_tranche2")
	default Column performanceTranche2() {
		return elapsedTime().toCase()
				.when(lt(5), "1")
				.when(ge(5).and(lt(10)), "2")
				.when(ge(10), "3").compose();
	}
}
