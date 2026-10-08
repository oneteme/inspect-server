package org.usf.inspect.server.erm;

import org.usf.jquery.core.Column;
import org.usf.jquery.core.JoinGroup;
import org.usf.jquery.core.ViewColumn;
import org.usf.jquery.mvc.Bind;
import org.usf.jquery.mvc.DatasetCatalog;
import org.usf.jquery.mvc.Expose;
import org.usf.jquery.mvc.Typed;

import static org.usf.inspect.server.config.constant.FieldConstant.*;
import static org.usf.jquery.core.JDBCType.UUID;
import static org.usf.jquery.core.Join.innerJoin;
import static org.usf.jquery.core.JoinGroup.joins;
import static org.usf.jquery.core.Predicate.*;

public interface MainSessionCatalog extends SessionCatalog {

	@Bind(VA_NAM)
	ViewColumn name();

	@Bind(VA_TYP)
	ViewColumn type();

	@Bind(VA_LCT)
	ViewColumn location();

	@Bind(VA_THR)
	ViewColumn thread();

	@Bind(VA_MSK)
	ViewColumn mask();

	@Bind(CD_STT)
	ViewColumn status();

	@Bind(CD_INS)
	@Expose(identity = "instance_env")
	@Typed(UUID)
	ViewColumn instanceEnv();

	default JoinGroup instance() {
		var instance = getStore().instance();
		return joins(innerJoin(instance.getView(), instanceEnv().eq(instance.id())));
	}

	default JoinGroup restRequest() {
		var restRequest = getStore().restRequest();
		return joins(innerJoin(restRequest.getView(), id().eq(restRequest.parent())));
	}

	@Expose(identity = "session_event")
	default JoinGroup sessionEvent() {
		var sessionEvent = getStore().sessionEvent();
		return joins(innerJoin(sessionEvent.getView(), id().eq(sessionEvent.sessionId())));
	}
}
