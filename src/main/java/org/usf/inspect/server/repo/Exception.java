package org.usf.inspect.server.repo;

import static org.usf.jquery.core.Predicate.isNotNull;

import org.usf.jquery.core.Column;
import org.usf.jquery.core.ViewColumn;
import org.usf.jquery.mvc.Bind;
import org.usf.jquery.mvc.DatasetResource;
import org.usf.jquery.mvc.Expose;

import static org.usf.inspect.server.config.constant.FieldConstant.*;

public interface Exception extends DatasetResource {
	
	@Bind(VA_TYP)
	ViewColumn type();
	
	@Bind(VA_ERR_TYP)
	@Expose(identity = "err_type")
	ViewColumn errType();
	
	@Bind(VA_ERR_MSG)
	ViewColumn errMsg();
	
	@Bind(VA_STK)
	ViewColumn stacktrace();
	
	@Bind(CD_ORD)
	ViewColumn order();
	
	@Bind(CD_RQT)
	ViewColumn parent();

	@Expose(identity = "count_exception")
	default Column countExceptions() {
		return errType().toCase().when(isNotNull(), 1).orElse(0).sum();
	}
}
