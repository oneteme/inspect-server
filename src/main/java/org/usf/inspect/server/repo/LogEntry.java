package org.usf.inspect.server.repo;

import static org.usf.inspect.server.config.constant.FieldConstant.CD_INS;
import static org.usf.inspect.server.config.constant.FieldConstant.CD_PRN_SES;
import static org.usf.inspect.server.config.constant.FieldConstant.DH_STR;
import static org.usf.inspect.server.config.constant.FieldConstant.VA_LVL;
import static org.usf.inspect.server.config.constant.FieldConstant.VA_MSG;
import static org.usf.inspect.server.config.constant.FieldConstant.VA_STK;

import org.usf.jquery.core.ViewColumn;
import org.usf.jquery.mvc.Bind;
import org.usf.jquery.mvc.DatasetCatalogue;
import org.usf.jquery.mvc.Expose;

public interface LogEntry extends DatasetCatalogue {
	
	@Bind(VA_LVL)
	@Expose(identity = "log_level")
	ViewColumn logLevel();
	
	@Bind(VA_MSG)
	@Expose(identity = "log_message")
	ViewColumn logMessage();
	
	@Bind(VA_STK)
	ViewColumn stacktrace();
	
	@Bind(DH_STR)
	ViewColumn start();
	
	@Bind(CD_PRN_SES)
	ViewColumn parent();
	
	@Bind(CD_INS)
	@Expose(identity = "instance_env")
	ViewColumn instanceEnv();
}
