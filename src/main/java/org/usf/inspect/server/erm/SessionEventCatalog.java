package org.usf.inspect.server.erm;

import org.usf.jquery.core.ViewColumn;
import org.usf.jquery.mvc.Bind;
import org.usf.jquery.mvc.DatasetCatalog;
import org.usf.jquery.mvc.Typed;

import static org.usf.inspect.server.config.constant.FieldConstant.*;
import static org.usf.jquery.core.JDBCType.UUID;

public interface SessionEventCatalog extends DatasetCatalog<InspectStore> {

    @Bind(DH_STR)
    ViewColumn start();

    @Bind(VA_TYP)
    ViewColumn type();

    @Bind(VA_CNT)
    ViewColumn value();

    @Bind(VA_LCT)
    ViewColumn location();

    @Bind(CD_PRN_SES)
    @Typed(UUID)
    ViewColumn sessionId();
}
