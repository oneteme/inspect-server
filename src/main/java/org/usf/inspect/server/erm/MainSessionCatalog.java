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
import static org.usf.jquery.core.Predicate.*;
import static org.usf.jquery.core.Predicate.ge;
import static org.usf.jquery.mvc.StoreManager.getInstance;

import org.usf.jquery.core.Column;
import org.usf.jquery.core.JoinGroup;
import org.usf.jquery.core.ViewColumn;
import org.usf.jquery.mvc.Bind;
import org.usf.jquery.mvc.DatasetCatalog;
import org.usf.jquery.mvc.Expose;
import org.usf.jquery.mvc.Typed;

public interface MainSessionCatalog extends DatasetCatalog<InspectStore> {

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
		var instance = getStore().instance();
		return joins(innerJoin(instance.getView(), instanceEnv().eq(instance.id())));
	}

	default JoinGroup restRequest() {
		var restRequest = getStore().restRequest();
		return joins(innerJoin(restRequest.getView(), id().eq(restRequest.parent())));
	}

	@Expose(identity = "user_action")
	default JoinGroup userAction() {
		var userAction = getStore().userAction();
		return joins(innerJoin(userAction.getView(), id().eq(userAction.parent())));
	}

	@Expose(identity = "elapsed_time")
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
				.when(end().isNull(), null)
				.when(errType().notNull(), 500)
				.when(errType().isNull(), 200)
				.compose().as("status");
	}

	@Expose(identity = "performance_tranche")
	default Column performanceTranche1() {
		return elapsedTime().toCase()
				.when(lt(1), "1")
				.when(ge(1).and(lt(3)), "2")
				.when(ge(3).and(lt(5)), "3")
				.when(ge(5).and(lt(10)), "4")
				.when(ge(10), "5").compose();
	}

	@Expose(identity = "performance_tranche2")
	default Column performanceTranche2() {
		return elapsedTime().toCase()
				.when(lt(5), "1")
				.when(ge(5).and(lt(10)), "2")
				.when(ge(10), "3").compose();
	}
}
