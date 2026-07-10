package org.usf.inspect.server.repo;

import static org.usf.inspect.core.RequestMask.FTP;
import static org.usf.inspect.server.config.constant.FieldConstant.CD_PRN_SES;
import static org.usf.inspect.server.config.constant.FieldConstant.CD_PRT;
import static org.usf.inspect.server.config.constant.FieldConstant.ID_FTP_RQT;
import static org.usf.inspect.server.config.constant.FieldConstant.VA_CLT_VRS;
import static org.usf.inspect.server.config.constant.FieldConstant.VA_CMD;
import static org.usf.inspect.server.config.constant.FieldConstant.VA_FAIL;
import static org.usf.inspect.server.config.constant.FieldConstant.VA_PCL;
import static org.usf.inspect.server.config.constant.FieldConstant.VA_SRV_VRS;
import static org.usf.inspect.server.config.constant.FieldConstant.VA_THR;
import static org.usf.inspect.server.config.constant.FieldConstant.VA_USR;
import static org.usf.jquery.core.Join.leftJoin;
import static org.usf.jquery.core.JoinGroup.joins;
import static org.usf.jquery.mvc.StoreManager.getInstance;

import org.usf.jquery.core.Column;
import org.usf.jquery.core.JoinGroup;
import org.usf.jquery.core.ViewColumn;
import org.usf.jquery.mvc.Bind;

public interface FtpRequestCatalog extends RequestCatalog {

	@Bind(ID_FTP_RQT)
	ViewColumn id();
	
	@Bind(VA_PCL)
	ViewColumn protocol();
	
	@Bind(VA_SRV_VRS)
	ViewColumn serverVersion();
	
	@Bind(VA_CLT_VRS)
	ViewColumn clientVersion();
	
	@Bind(VA_CMD)
	ViewColumn command();
	
	@Bind(VA_FAIL)
	ViewColumn failed();
	
	@Bind(CD_PRN_SES)
	ViewColumn parent();
	
	default Column elapsedTime() {
		return end().minus(start()).epoch();
	}
	
	default JoinGroup exception() {
		var exception = getInstance().getStore(InspectStore.class).exception();
		return joins(leftJoin(exception.getView(), exception.parent().eq(id()), exception.type().eq(FTP.name())));
	}
}
