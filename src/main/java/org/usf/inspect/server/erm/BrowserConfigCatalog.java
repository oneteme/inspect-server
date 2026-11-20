package org.usf.inspect.server.erm;

import org.usf.jquery.core.ViewColumn;
import org.usf.jquery.mvc.Bind;
import org.usf.jquery.mvc.DatasetCatalog;
import org.usf.jquery.mvc.Typed;

import static org.usf.inspect.server.config.constant.FieldConstant.*;
import static org.usf.jquery.core.JDBCType.UUID;


public interface BrowserConfigCatalog extends DatasetCatalog<InspectStore> {

	@Bind(VA_DVC_DSP_RSL)
	ViewColumn deviceDisplayResolution();

	@Bind(VA_DVC_ORN)
	ViewColumn deviceOrientation();

	@Bind(VA_DVC_CNT)
	ViewColumn deviceConnectivity();

	@Bind(VA_SAV_DTA)
	ViewColumn saveData();

	@Bind(VA_WDW_VPT_BDS)
	ViewColumn windowViewportBounds();

	@Bind(VA_WDW_ZOM_LVL)
	ViewColumn windowZoomLevel();

	@Bind(VA_USR_LNG)
	ViewColumn userLanguage();

	@Bind(VA_USR_THM)
	ViewColumn userTheme();

	@Bind(CD_INS)
	@Typed(UUID)
	ViewColumn instanceId();
}
