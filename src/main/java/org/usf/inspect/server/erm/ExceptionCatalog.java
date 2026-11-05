package org.usf.inspect.server.erm;

import static org.usf.jquery.core.JDBCType.*;
import static org.usf.jquery.core.Predicate.isNotNull;

import org.usf.jquery.core.Column;
import org.usf.jquery.core.ViewColumn;
import org.usf.jquery.mvc.Bind;
import org.usf.jquery.mvc.DatasetCatalog;
import org.usf.jquery.mvc.Expose;
import org.usf.jquery.mvc.Typed;

import static org.usf.inspect.server.config.constant.FieldConstant.*;

public interface ExceptionCatalog extends DatasetCatalog<InspectStore> {


	@Bind(VA_TRC_TYP)
	@Expose(identity = "trace_type")
	ViewColumn traceType();

	@Bind(VA_ERR_TYP)
	@Expose(identity = "err_type")
	ViewColumn errType();
	
	@Bind(VA_ERR_MSG)
	@Expose(identity = "err_msg")
	ViewColumn errMsg();
	
	@Bind(VA_STK)
	ViewColumn stacktrace();

	@Bind(VA_CAS)
	ViewColumn cause();

	@Bind(CD_ORD)
	@Expose(identity = "err_order")
	ViewColumn offset();
	
	@Bind("cd_trc")
	@Typed(UUID)
	ViewColumn parent();

	@Expose(identity = "count_exception")
	default Column countExceptions() {
		return errType().toCase().when(isNotNull(), 1).orElse(0).sum();
	}

	@Expose(identity = "has_stack")
	default Column hasStack() {
		return stacktrace().toCase().when(isNotNull(), true).orElse(false);
	}
}
