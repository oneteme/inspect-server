package org.usf.inspect.server.repo;

import static org.usf.inspect.server.config.constant.FieldConstant.DH_END;
import static org.usf.inspect.server.config.constant.FieldConstant.DH_STR;
import static org.usf.inspect.server.config.constant.FieldConstant.VA_ERR_TYP;
import static org.usf.jquery.core.Predicate.isNotNull;

import org.usf.jquery.core.Column;
import org.usf.jquery.core.Predicate;
import org.usf.jquery.core.ViewColumn;
import org.usf.jquery.web.proxy.Bind;

public interface CommunColumns {

	@Bind(DH_STR)
	ViewColumn start();
	
	@Bind(DH_END)
	ViewColumn end();
	
	@Bind(VA_ERR_TYP)
	ViewColumn errType();
	
	default Column elapsedTime() {
		return end().minus(start()).epoch();
	}
	
	default Column countExceptions() {
		return errType().toCase().when(isNotNull(), 1).orElse(0);
	}
	
}
