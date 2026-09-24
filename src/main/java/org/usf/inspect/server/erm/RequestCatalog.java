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
import static org.usf.jquery.core.Join.leftJoin;
import static org.usf.jquery.core.JoinGroup.joins;
import static org.usf.jquery.core.Predicate.eq;
import static org.usf.jquery.core.Predicate.ge;

public interface RequestCatalog extends DatasetCatalog<InspectStore> {

    ViewColumn id();

    @Bind(VA_HST)
    ViewColumn host();

    @Bind(CD_PRT)
    ViewColumn port();

    @Bind(DH_STR)
    ViewColumn start();

    @Bind(DH_END)
    ViewColumn end();

    @Bind(VA_USR)
    ViewColumn user();

    @Bind(CD_STT)
    ViewColumn status();

    @Bind(VA_THR)
    ViewColumn thread();

    @Bind(CD_INS)
    @Expose(identity = "instance_env")
    @Typed(UUID)
    ViewColumn instanceEnv();

    @Expose(identity = "elapsed_time")
	default Column elapsedTime() {
		return end().minus(start()).epoch();
	}

    @Expose(identity = "count_request_error")
    default Column countError() {
        return status().toCase().when(eq(0).or(ge(400)), true).compose().count();
    }

    default JoinGroup instance() {
        var instance = getStore().instance();
        return joins(innerJoin(instance.getView(), instanceEnv().eq(instance.id())));
    }
	
	default JoinGroup exception() { //TODO parameterized resource =>  exception(RequestMask)
		var exception = getStore().exception();
		return joins(leftJoin(exception.getView(), id().eq(exception.parent())));
	}
}
