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
import static org.usf.jquery.core.Predicate.*;

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

    @Bind(CD_PRN_SES)
    @Typed(UUID)
    ViewColumn parent();

    @Bind(CD_INS)
    @Expose(identity = "instance_env")
    @Typed(UUID)
    ViewColumn instanceEnv();

    @Expose(identity = "elapsed_time")
	default Column elapsedTime() {
		return end().minus(start()).epoch();
	}

    @Expose(identity = "count_error")
    default Column countError() {
        return status().toCase().when(eq(0).or(ge(400)), true).compose().count();
    }

    @Expose(identity = "count_error_server")
    default Column countErrorServer() {
        return status().toCase().when(ge(500), true).compose().count();
    }

    @Expose(identity = "count_error_client")
    default Column countErrorClient() {
        return status().toCase().when(ge(400).and(lt(500)), true).compose().count();
    }

    @Expose(identity = "error_type")
    default Column errorTypeExpressions() {
        return status().toCase()
                .when(lt(100), "CNX_ERR")
                .when(ge(400).and(lt(500)), "APP_ERR")
                .when(ge(500).and(lt(600)), "INT_ERR")
                .when(ge(600), "DEV_ERR")
                .orElse(null);
    }

    @Expose(identity = "status_tranche")
    default Column statusTranche() {
        return status().toCase()
                .when(eq(0), "1")
                .when(ge(100).and(lt(200)), "2")
                .when(ge(200).and(lt(300)), "3")
                .when(ge(300).and(lt(400)), "4")
                .when(ge(400).and(lt(500)), "5")
                .when(ge(500), "6").compose();
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

    default JoinGroup instance() {
        var instance = getStore().instance();
        return joins(innerJoin(instance.getView(), instanceEnv().eq(instance.id())));
    }
}
