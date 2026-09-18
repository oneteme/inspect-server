package org.usf.inspect.server.erm;

import org.usf.inspect.core.SessionMask;
import org.usf.jquery.core.JoinGroup;
import org.usf.jquery.core.ViewColumn;
import org.usf.jquery.mvc.Bind;
import org.usf.jquery.mvc.DatasetCatalog;

import static org.usf.inspect.server.config.constant.FieldConstant.CD_ORD;
import static org.usf.inspect.server.config.constant.FieldConstant.VA_NAM;
import static org.usf.jquery.core.Join.leftJoin;
import static org.usf.jquery.core.JoinGroup.joins;
import static org.usf.jquery.mvc.StoreManager.getInstance;

public interface StageCatalog extends DatasetCatalog<InspectStore> {
    @Bind(VA_NAM)
    ViewColumn name();

    @Bind(CD_ORD)
    ViewColumn order();

    ViewColumn parent();

    default JoinGroup exception() {
        var exception = getStore().exception();
        return joins(leftJoin(exception.getView(), parent().eq(exception.parent()), exception.type().eq(getRequestType().name())));
    }

    SessionMask getRequestType();
}
