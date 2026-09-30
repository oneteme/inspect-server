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
import static org.usf.jquery.core.Predicate.ge;

public interface SessionCatalog extends DatasetCatalog<InspectStore> {
    @Bind(ID_SES)
    @Typed(UUID)
    ViewColumn id();

    @Bind(DH_STR)
    ViewColumn start();

    @Bind(DH_END)
    ViewColumn end();

    @Bind(VA_USR)
    ViewColumn user();

    @Bind(CD_STT)
    ViewColumn status();

    @Bind(VA_MSK)
    ViewColumn mask();

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

    @Expose(identity = "count_error_server")
    default Column countErrorServerStatus() {
        return status().toCase().when(ge(500), status()).compose().count();
    }

    @Expose(identity = "count_error_client")
    default Column countClientErrorStatus() {
        return status().toCase().when(ge(400).and(lt(500)), status()).compose().count();
    }

    @Expose(identity = "count_error")
    default Column countError() {
        return status().toCase().when(eq(0).or(ge(400)), status()).compose().count();
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

    default JoinGroup restRequest() {
        var restRequest = getStore().restRequest();
        return joins(innerJoin(restRequest.getView(), id().eq(restRequest.parent())));
    }
}
