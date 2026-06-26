package org.usf.inspect.server.repo;

import static org.usf.inspect.server.config.constant.FieldConstant.CD_INS;
import static org.usf.inspect.server.config.constant.FieldConstant.DH_END;
import static org.usf.inspect.server.config.constant.FieldConstant.DH_STR;
import static org.usf.inspect.server.config.constant.FieldConstant.ID_SES;
import static org.usf.inspect.server.config.constant.FieldConstant.VA_ERR_MSG;
import static org.usf.inspect.server.config.constant.FieldConstant.VA_ERR_TYP;
import static org.usf.inspect.server.config.constant.FieldConstant.VA_LCT;
import static org.usf.inspect.server.config.constant.FieldConstant.VA_MSK;
import static org.usf.inspect.server.config.constant.FieldConstant.VA_NAM;
import static org.usf.inspect.server.config.constant.FieldConstant.VA_STK;
import static org.usf.inspect.server.config.constant.FieldConstant.VA_THR;
import static org.usf.inspect.server.config.constant.FieldConstant.VA_TYP;
import static org.usf.inspect.server.config.constant.FieldConstant.VA_USR;
import static org.usf.jquery.core.Join.innerJoin;
import static org.usf.jquery.core.JoinGroup.joins;
import static org.usf.jquery.core.Predicate.isNotNull;
import static org.usf.jquery.web.proxy.StoreManager.getInstance;

import org.usf.jquery.core.Column;
import org.usf.jquery.core.JoinGroup;
import org.usf.jquery.core.ViewColumn;
import org.usf.jquery.web.proxy.Bind;
import org.usf.jquery.web.proxy.DatasetResource;
import org.usf.jquery.web.proxy.Expose;

//@IncludeResources({PeriodColumns.class})
public interface MainSession extends DatasetResource {

	@Bind(ID_SES)
	ViewColumn id();

	@Bind(VA_USR)
	ViewColumn user();

	@Bind(VA_NAM)
	ViewColumn name();

	@Bind(DH_STR)
	ViewColumn start();

	@Bind(DH_END)
	ViewColumn end();

	@Bind(VA_TYP)
	ViewColumn type();

	@Bind(VA_LCT)
	ViewColumn location();

	@Bind(VA_THR)
	ViewColumn thread();

	@Bind(VA_ERR_TYP)
	@Expose(identity = "err_type")
	ViewColumn errType();

	@Bind(VA_ERR_MSG)
	@Expose(identity = "err_msg")
	ViewColumn errMsg();

	@Bind(VA_STK)
	ViewColumn stacktrace();

	@Bind(VA_MSK)
	ViewColumn mask();

	@Bind(CD_INS)
	@Expose(identity = "instance_env")
	ViewColumn instanceEnv();

	default JoinGroup instance() {
		var instance = getInstance().getStore(InspectStore.class).instance();
		return joins(innerJoin(instance.getView(), instanceEnv().eq(instance.id())));
	}

	default Column elapsedTime() {
		return end().minus(start()).epoch();
	}
	
	@Expose(identity = "count_exception")
	default Column countExceptions() {
		return errType().toCase().when(isNotNull(), 1).orElse(0).sum();
	}
}
