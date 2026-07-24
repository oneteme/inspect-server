package org.usf.inspect.server.erm;

import static org.usf.inspect.core.RequestMask.JDBC;
import static org.usf.inspect.server.config.constant.FieldConstant.*;
import static org.usf.jquery.core.JDBCType.UUID;
import static org.usf.jquery.core.Predicate.*;
import static org.usf.jquery.core.Predicate.lt;

import org.usf.inspect.core.RequestMask;
import org.usf.jquery.core.Column;
import org.usf.jquery.core.ViewColumn;
import org.usf.jquery.mvc.Bind;
import org.usf.jquery.mvc.Expose;
import org.usf.jquery.mvc.Typed;

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
	
	@Bind(VA_FAIL)
	ViewColumn failed();
	
	@Bind(CD_PRN_SES)
	@Typed(UUID)
	ViewColumn parent();
	
	@Override
	default RequestMask getRequestType() {
		return JDBC;
	}

	@Expose(identity = "count_request_error")
	default Column countError() {
		return failed().toCase().when(eq(true), failed()).compose().count();
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
