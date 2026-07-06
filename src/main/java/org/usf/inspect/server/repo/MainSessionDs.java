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
import static org.usf.jquery.core.JDBCType.UUID;
import static org.usf.jquery.core.JDBCType.VARCHAR;
import static org.usf.jquery.core.Join.innerJoin;
import static org.usf.jquery.core.JoinGroup.joins;
import static org.usf.jquery.core.Predicate.isNotNull;
import static org.usf.jquery.core.Predicate.isNull;
import static org.usf.jquery.mvc.StoreManager.getInstance;

import org.usf.jquery.core.Column;
import org.usf.jquery.core.JDBCType;
import org.usf.jquery.core.JoinGroup;
import org.usf.jquery.core.Predicate;
import org.usf.jquery.core.ViewColumn;
import org.usf.jquery.mvc.Bind;
import org.usf.jquery.mvc.DatasetCatalogue;
import org.usf.jquery.mvc.Expose;
import org.usf.jquery.mvc.Typed;

//@IncludeResources({PeriodColumns.class})
public interface MainSessionDs extends DatasetCatalogue {

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
	ViewColumn errorType();

	@Bind(VA_ERR_MSG)
	@Expose(identity = "err_msg")
	ViewColumn errorMessage();

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
		return errorType().toCase().when(isNotNull(), 1).orElse(0).sum();
	}
	
	@Expose(identity = "status_main_tranche")
    default Column statusMainTranche() {
        return errorType().toCase().when(isNull(), "false").orElse("true");
    }
}
