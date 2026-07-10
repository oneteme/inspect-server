package org.usf.inspect.server.repo;

import org.usf.jquery.core.Column;
import org.usf.jquery.core.JoinGroup;
import org.usf.jquery.core.ViewColumn;
import org.usf.jquery.mvc.Bind;
import org.usf.jquery.mvc.DatasetCatalog;
import org.usf.jquery.mvc.Expose;

import static org.usf.inspect.core.RequestMask.LOCAL;
import static org.usf.inspect.server.config.constant.FieldConstant.*;
import static org.usf.jquery.core.Join.innerJoin;
import static org.usf.jquery.core.Join.leftJoin;
import static org.usf.jquery.core.JoinGroup.joins;
import static org.usf.jquery.mvc.StoreManager.getInstance;

public interface LocalRequestCatalog extends DatasetCatalog {

	@Bind(ID_LCL_RQT)
	ViewColumn id();
	
	@Bind(VA_NAM)
	ViewColumn name();
	
	@Bind(VA_TYP)
	ViewColumn type();
	
	@Bind(VA_LCT)
	ViewColumn location();
	
	@Bind(DH_STR)
	ViewColumn start();
	
	@Bind(DH_END)
	ViewColumn end();
	
	@Bind(VA_USR)
	ViewColumn user();
	
	@Bind(VA_THR)
	ViewColumn thread();
	
	@Bind(VA_FAIL)
	ViewColumn failed();
	
	@Bind(CD_PRN_SES)
	ViewColumn parent();
	
	@Bind(CD_INS)
	@Expose(identity = "instance_env")
	ViewColumn instanceEnv();

	default JoinGroup instance() {
		var instance = getInstance().getStore(InspectStore.class).instance();
		return joins(innerJoin(instance.getView(), instanceEnv().eq(instance.id())));
	}

	default JoinGroup exception() {
		var exception = getInstance().getStore(InspectStore.class).exception();
		return joins(leftJoin(exception.getView(), exception.parent().eq(id()), exception.type().eq(LOCAL.name())));
	}

	default Column elapsedTime() {
		return end().minus(start()).epoch();
	}
}
