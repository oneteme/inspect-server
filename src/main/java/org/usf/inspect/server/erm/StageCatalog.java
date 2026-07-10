package org.usf.inspect.server.erm;

import org.usf.jquery.core.ViewColumn;
import org.usf.jquery.mvc.Bind;
import org.usf.jquery.mvc.DatasetCatalog;

import static org.usf.inspect.server.config.constant.FieldConstant.CD_ORD;
import static org.usf.inspect.server.config.constant.FieldConstant.VA_NAM;

public interface StageCatalog extends DatasetCatalog {
    @Bind(VA_NAM)
    ViewColumn name();

    @Bind(CD_ORD)
    ViewColumn order();
}
