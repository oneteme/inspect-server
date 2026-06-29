package org.usf.inspect.server.repo;

import static org.usf.inspect.server.config.constant.FieldConstant.CD_PRN_SES;
import static org.usf.inspect.server.config.constant.FieldConstant.DH_STR;
import static org.usf.inspect.server.config.constant.FieldConstant.VA_NAM;
import static org.usf.inspect.server.config.constant.FieldConstant.VA_NDE_NAM;
import static org.usf.inspect.server.config.constant.FieldConstant.VA_TYP;

import org.usf.jquery.core.ViewColumn;
import org.usf.jquery.mvc.Bind;
import org.usf.jquery.mvc.DatasetResource;

public interface UserAction extends DatasetResource {
	
	@Bind(VA_TYP)
	ViewColumn type();
	
	@Bind(VA_NAM)
	ViewColumn name();
	
	@Bind(VA_NDE_NAM)
	ViewColumn nodeName();
	
	@Bind(DH_STR)
	ViewColumn start();
	
	@Bind(CD_PRN_SES)
	ViewColumn parent();
}
