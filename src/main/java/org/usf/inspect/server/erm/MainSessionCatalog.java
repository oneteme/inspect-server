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

	@Expose(identity = "user_action")
	default JoinGroup userAction() {
		var userAction = getStore().userAction();
		return joins(innerJoin(userAction.getView(), id().eq(userAction.parent())));
	}
}
