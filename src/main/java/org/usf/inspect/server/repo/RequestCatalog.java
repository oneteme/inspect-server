package org.usf.inspect.server.repo;

import org.usf.jquery.core.Join;
import org.usf.jquery.core.ViewColumn;
import org.usf.jquery.mvc.Bind;
import org.usf.jquery.mvc.DatasetCatalog;
import org.usf.jquery.mvc.Expose;

import static org.usf.inspect.server.config.constant.FieldConstant.*;
import static org.usf.jquery.core.Join.innerJoin;
import static org.usf.jquery.mvc.StoreManager.getInstance;

public interface RequestCatalog extends DatasetCatalog {

    @Expose(false)
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

    @Bind(VA_THR)
    ViewColumn thread();

    @Bind(CD_INS)
    @Expose(identity = "instance_env")
    ViewColumn instanceEnv();

    default Join instance() {
        var instance = getInstance().getStore(InspectStore.class).instance();
        return innerJoin(instance.getView(), instanceEnv().eq(instance.id()));
    }
}
