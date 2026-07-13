package org.usf.inspect.server.erm;

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
import static org.usf.jquery.core.JDBCType.UUID;
import static org.usf.jquery.core.Join.innerJoin;
import static org.usf.jquery.core.JoinGroup.joins;
import static org.usf.jquery.core.Predicate.isNotNull;
import static org.usf.jquery.core.Predicate.isNull;
import static org.usf.jquery.mvc.StoreManager.getInstance;

import org.usf.jquery.core.Column;
import org.usf.jquery.core.JoinGroup;
import org.usf.jquery.core.ViewColumn;
import org.usf.jquery.mvc.Bind;
import org.usf.jquery.mvc.DatasetCatalog;
import org.usf.jquery.mvc.Expose;
import org.usf.jquery.mvc.Typed;

//@IncludeResources({PeriodColumns.class})
public interface MainSessionCatalog extends DatasetCatalog {

	@Bind(ID_SES)
	@Typed(UUID)
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
	@Typed(UUID)
	ViewColumn instanceEnv();

	default JoinGroup instance() {
		var instance = getInstance().getStore(InspectStore.class).instance();
		return joins(innerJoin(instance.getView(), instanceEnv().eq(instance.id())));
	}

	@Expose(identity = "user_action")
	default JoinGroup userAction() {
		var userAction = getInstance().getStore(InspectStore.class).userAction();
		return joins(innerJoin(userAction.getView(), id().eq(userAction.parent())));
	}

	default Column elapsedTime() {
		return end().minus(start()).epoch();
	}
	
	@Expose(identity = "count_exception")
	default Column countExceptions() {
		return errType().toCase().when(isNotNull(), 1).orElse(0).sum(); //TODO errType().count exclude null
	}
	
	@Expose(identity = "status_main_tranche")
    default Column statusMainTranche() {
        return errType().toCase().when(isNull(), "false").orElse("true"); //TODO errType().notNull()
    }

	default Column status() {
		return Column.beginCase()
				.when(end().isNull(), -1)
				.when(errType().notNull(), 1)
				.when(errType().isNull(), 0)
				.compose(null).as("status");
	}
}
