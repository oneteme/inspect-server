package org.usf.inspect.server.repo;

import org.usf.jquery.core.Column;
import org.usf.jquery.core.Predicate;
import org.usf.jquery.core.ViewColumn;
import org.usf.jquery.mvc.Bind;
import org.usf.jquery.mvc.Expose;

import static org.usf.inspect.server.config.constant.FieldConstant.*;
import static org.usf.jquery.core.Predicate.*;
import static org.usf.jquery.mvc.StoreManager.getInstance;


public interface CommunColumns {


	
	@Bind(CD_PRT)
	ViewColumn port();
	
	@Bind(VA_ERR_TYP)
	ViewColumn errType();
	
//	@Bind(VA_NAM)
//	ViewColumn name();
	
	@Bind(VA_USR)
	ViewColumn user();
	
//	@Bind(VA_TYP)
//	ViewColumn type();
	
	@Bind(VA_THR)
	ViewColumn thread();
	
//	@Bind(CD_ORD)
//	ViewColumn order();
	
	@Bind(VA_CMD)
	ViewColumn command();
	
	@Bind(VA_FAIL)
	ViewColumn failed();
	
	@Bind(CD_INS)
	@Expose(identity = "instance_env")
	ViewColumn instanceEnv();
	
	@Bind(DH_STR)
	ViewColumn start();
	
	@Bind(DH_END)
	ViewColumn end();
	
	@Bind(CD_STT)
	ViewColumn status();
	
	default Column elapsedTime() {
		return end().minus(start()).epoch();
	}
	
	default Column countExceptions() {
		return errType().toCase().when(isNotNull(), 1).orElse(0).sum();
	}
	
	
	@Expose(identity = "error_type_session")
    default Column errorTypeExpressionsSession() {
		var restSession = getInstance().getStore(InspectStore.class).restSession();
        return status().toCase()
                .when(ge(200).and(lt(400)), null)
                .when(ge(400).and(lt(500)), "ClientError")
                .orElse(restSession.errType());
    }
	
    private static Column countStatusByType(ViewColumn status, Predicate op) {
        return status.toCase().when(op, status).orElse(null).count();
    }

}
