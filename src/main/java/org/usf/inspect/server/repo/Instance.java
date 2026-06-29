package org.usf.inspect.server.repo;

import static org.usf.inspect.server.config.constant.FieldConstant.DH_END;
import static org.usf.inspect.server.config.constant.FieldConstant.DH_STR;
import static org.usf.inspect.server.config.constant.FieldConstant.ID_INS;
import static org.usf.inspect.server.config.constant.FieldConstant.VA_ADD_PRP;
import static org.usf.inspect.server.config.constant.FieldConstant.VA_ADR;
import static org.usf.inspect.server.config.constant.FieldConstant.VA_APP;
import static org.usf.inspect.server.config.constant.FieldConstant.VA_BRCH;
import static org.usf.inspect.server.config.constant.FieldConstant.VA_CLR;
import static org.usf.inspect.server.config.constant.FieldConstant.VA_CNF;
import static org.usf.inspect.server.config.constant.FieldConstant.VA_ENV;
import static org.usf.inspect.server.config.constant.FieldConstant.VA_HSH;
import static org.usf.inspect.server.config.constant.FieldConstant.VA_OS;
import static org.usf.inspect.server.config.constant.FieldConstant.VA_RE;
import static org.usf.inspect.server.config.constant.FieldConstant.VA_RSR;
import static org.usf.inspect.server.config.constant.FieldConstant.VA_TYP;
import static org.usf.inspect.server.config.constant.FieldConstant.VA_USR;
import static org.usf.inspect.server.config.constant.FieldConstant.VA_VRS;

import org.usf.jquery.core.Column;
import org.usf.jquery.core.ViewColumn;
import org.usf.jquery.mvc.Bind;
import org.usf.jquery.mvc.DatasetResource;
import org.usf.jquery.mvc.Expose;

public interface Instance extends DatasetResource {

	@Bind(ID_INS)
	ViewColumn id();
	
	@Bind(VA_TYP)
	ViewColumn type();
	
	@Bind(DH_STR)
	ViewColumn start();
	
	@Bind(DH_END)
	ViewColumn end();
	
	@Bind(VA_APP)
	@Expose(identity = "app_name")
	ViewColumn appName();
	
	@Bind(VA_VRS)
	ViewColumn version();
	
	@Bind(VA_ADR)
	ViewColumn address();
	
	@Bind(VA_ENV)
	ViewColumn environement();
	
	@Bind(VA_OS)
	ViewColumn os();
	
	@Bind(VA_RE)
	ViewColumn re();
	
	@Bind(VA_USR)
	ViewColumn user();
	
	@Bind(VA_CLR)
	ViewColumn collector();
	
	@Bind(VA_BRCH)
	ViewColumn branch();
	
	@Bind(VA_HSH)
	ViewColumn hash();
	
	@Bind(VA_CNF)
	ViewColumn configuration();
	
	@Bind(VA_RSR)
	ViewColumn resource();
	
	@Bind(VA_ADD_PRP)
	ViewColumn additionalProperties();
	
	default Column elapsedTime() {
		return end().minus(start()).epoch();
	}
}
