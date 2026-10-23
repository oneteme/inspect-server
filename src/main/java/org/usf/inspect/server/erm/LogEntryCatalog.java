package org.usf.inspect.server.erm;

import static org.usf.inspect.server.config.constant.FieldConstant.*;
import static org.usf.jquery.core.JDBCType.UUID;
import static org.usf.jquery.core.Join.innerJoin;
import static org.usf.jquery.core.JoinGroup.joins;

import org.usf.jquery.core.JoinGroup;
import org.usf.jquery.core.ViewColumn;
import org.usf.jquery.mvc.Bind;
import org.usf.jquery.mvc.DatasetCatalog;
import org.usf.jquery.mvc.Expose;
import org.usf.jquery.mvc.Typed;

public interface LogEntryCatalog extends DatasetCatalog<InspectStore> {

	@Bind(VA_MSG)
	@Expose(identity = "log_message")
	ViewColumn logMessage();
	
	@Bind(VA_STK)
	ViewColumn stacktrace();

	@Bind(VA_THR)
	ViewColumn thread();

	@Bind(VA_ACT)
	ViewColumn action();
	
	@Bind(DH_STR)
	ViewColumn start();
	
	@Bind(CD_PRN_SES)
	@Typed(UUID)
	ViewColumn parent();
	
	@Bind(CD_INS)
	@Expose(identity = "instance_env")
	@Typed(UUID)
	ViewColumn instanceEnv();

	default JoinGroup instance() {
		var instance = getStore().instance();
		return joins(innerJoin(instance.getView(), instanceEnv().eq(instance.id())));
	}
}
