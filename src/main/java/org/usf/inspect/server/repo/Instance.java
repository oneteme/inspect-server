package org.usf.inspect.server.repo;

import static org.usf.inspect.server.config.constant.FieldConstant.*;

import org.usf.jquery.core.ViewColumn;
import org.usf.jquery.web.proxy.Bind;
import org.usf.jquery.web.proxy.DatasetResource;

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
}
