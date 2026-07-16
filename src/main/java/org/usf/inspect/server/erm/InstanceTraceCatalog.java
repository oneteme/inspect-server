package org.usf.inspect.server.erm;

import static org.usf.inspect.server.config.constant.FieldConstant.CD_INS;
import static org.usf.inspect.server.config.constant.FieldConstant.DH_STR;
import static org.usf.inspect.server.config.constant.FieldConstant.VA_ATP;
import static org.usf.inspect.server.config.constant.FieldConstant.VA_FILENAME;
import static org.usf.inspect.server.config.constant.FieldConstant.VA_PND;
import static org.usf.inspect.server.config.constant.FieldConstant.VA_TRC_CNT;
import static org.usf.jquery.core.JDBCType.UUID;

import org.usf.jquery.core.ViewColumn;
import org.usf.jquery.mvc.Bind;
import org.usf.jquery.mvc.DatasetCatalog;
import org.usf.jquery.mvc.Expose;
import org.usf.jquery.mvc.Typed;

public interface InstanceTraceCatalog extends DatasetCatalog {
	
	@Bind(VA_PND)
	ViewColumn pending();
	
	@Bind(VA_ATP)
	ViewColumn attempts();
	
	@Bind(VA_TRC_CNT)
	@Expose(identity = "trace_count")
	ViewColumn traceCount();
	
	@Bind(VA_FILENAME)
	ViewColumn filename();
	
	@Bind(DH_STR)
	ViewColumn start();
	
	@Bind(CD_INS)
	@Typed(UUID)
	@Expose(identity = "instance_env")
	ViewColumn instanceEnv();
}
