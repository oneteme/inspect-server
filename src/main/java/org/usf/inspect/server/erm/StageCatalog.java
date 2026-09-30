package org.usf.inspect.server.erm;

import org.usf.jquery.core.Column;
import org.usf.jquery.core.JoinGroup;
import org.usf.jquery.core.ViewColumn;
import org.usf.jquery.mvc.Bind;
import org.usf.jquery.mvc.DatasetCatalog;

import static org.usf.inspect.server.config.constant.FieldConstant.*;
import static org.usf.inspect.server.config.constant.FieldConstant.DH_END;
import static org.usf.jquery.core.Join.leftJoin;
import static org.usf.jquery.core.JoinGroup.joins;

public interface StageCatalog extends DatasetCatalog<InspectStore> {
    @Bind(DH_STR)
    ViewColumn start();

    @Bind(DH_END)
    ViewColumn end();

    @Bind(VA_NAM)
    ViewColumn name();

    @Bind(CD_ORD)
    ViewColumn order();

    ViewColumn parent();

    default JoinGroup exception() {
        var exception = getStore().exception();
        return joins(leftJoin(exception.getView(), parent().eq(exception.parent()), order().eq(exception.order())));
    }

    default Column elapsedTime() {
        return end().minus(start()).epoch();
    }
}
