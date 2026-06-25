package org.usf.inspect.server.repo;

import static org.usf.inspect.server.config.constant.FieldConstant.*;
import static org.usf.jquery.core.Predicate.isNotNull;

import org.usf.jquery.core.Column;
import org.usf.jquery.core.ViewColumn;
import org.usf.jquery.web.proxy.Bind;
import org.usf.jquery.web.proxy.Expose;

public interface CommunColumns {

	@Bind(VA_HST)
	ViewColumn host();
	
	@Bind(CD_PRT)
	ViewColumn port();
	
	@Bind(VA_ERR_TYP)
	ViewColumn errType();
	
	@Bind(VA_NAM)
	ViewColumn name();
	
	@Bind(VA_USR)
	ViewColumn user();
	
	@Bind(VA_TYP)
	ViewColumn type();
	
	@Bind(VA_THR)
	ViewColumn thread();
	
	@Bind(CD_ORD)
	ViewColumn order();
	
	@Bind(VA_CMD)
	ViewColumn command();
	
	@Bind(VA_FAIL)
	ViewColumn failed();
	
	@Bind(CD_INS)
	@Expose(identity = "instance")
	ViewColumn instanceEnv();
	
	@Bind(DH_STR)
	ViewColumn start();
	
	@Bind(DH_END)
	ViewColumn end();
	
	default Column elapsedTime() {
		return end().minus(start()).epoch();
	}
	
	default Column countExceptions() {
		return errType().toCase().when(isNotNull(), 1).orElse(0).sum();
	}
	
}
