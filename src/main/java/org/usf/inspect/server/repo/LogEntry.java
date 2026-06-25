package org.usf.inspect.server.repo;

import static org.usf.inspect.server.config.constant.FieldConstant.CD_INS;
import static org.usf.inspect.server.config.constant.FieldConstant.CD_PRN_SES;
import static org.usf.inspect.server.config.constant.FieldConstant.DH_STR;
import static org.usf.inspect.server.config.constant.FieldConstant.VA_LVL;
import static org.usf.inspect.server.config.constant.FieldConstant.VA_MSG;
import static org.usf.inspect.server.config.constant.FieldConstant.VA_STK;

import org.usf.jquery.core.ViewColumn;
import org.usf.jquery.web.proxy.Bind;
import org.usf.jquery.web.proxy.DatasetResource;
import org.usf.jquery.web.proxy.Expose;

public interface LogEntry extends DatasetResource {
	
	@Bind(VA_LVL)
	ViewColumn logLevel();
	
	@Bind(VA_MSG)
	ViewColumn logMessage();
	
	@Bind(VA_STK)
	ViewColumn stacktrace();
	
	@Bind(DH_STR)
	ViewColumn start();
	
	@Bind(CD_PRN_SES)
	ViewColumn parent();
	
	@Bind(CD_INS)
	@Expose(identity = "instance")
	ViewColumn instanceEnv();
}
