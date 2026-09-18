package org.usf.inspect.server.erm;

import static org.usf.inspect.server.config.constant.FieldConstant.DH_END;
import static org.usf.inspect.server.config.constant.FieldConstant.DH_STR;

import org.usf.jquery.core.Column;
import org.usf.jquery.core.ViewColumn;
import org.usf.jquery.mvc.Bind;

public interface MetricCatalog {


	@Bind(DH_STR)
	ViewColumn start();

	@Bind(DH_END)
	ViewColumn end();


	default Column elapsedTime() {
		return end().minus(start()).epoch();
	}
	
}
