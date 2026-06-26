package org.usf.inspect.server.repo;

import static org.usf.jquery.core.Join.innerJoin;
import static org.usf.jquery.core.Join.leftJoin;
import static org.usf.jquery.core.JoinGroup.joins;
import static org.usf.jquery.core.Predicate.eq;
import static org.usf.jquery.core.Predicate.ge;
import static org.usf.jquery.core.Predicate.lt;
import static org.usf.jquery.web.proxy.StoreManager.getInstance;


import org.usf.inspect.server.config.constant.DBColumn;
import org.usf.jquery.core.Column;
import org.usf.jquery.core.Join;
import org.usf.jquery.core.JoinGroup;
import org.usf.jquery.core.Predicate;
import org.usf.jquery.core.ViewColumn;
import org.usf.jquery.web.ViewDecorator;
import org.usf.jquery.web.proxy.Bind;
import org.usf.jquery.web.proxy.DatasetResource;
import org.usf.jquery.web.proxy.Expose;

import static org.usf.inspect.server.config.TraceApiColumn.ERR_TYPE;
import static org.usf.inspect.server.config.TraceApiColumn.STATUS;
import static org.usf.inspect.server.config.TraceApiTable.EXCEPTION;
import static org.usf.inspect.server.config.constant.FieldConstant.*;

public interface RestRequest extends DatasetResource {

	@Bind(ID_RST_RQT)
	ViewColumn id();
	
	@Bind(VA_MTH)
	ViewColumn method();
	
	@Bind(VA_PCL)
	ViewColumn protocol();
	
	@Bind(VA_HST)
	ViewColumn host();
	
	@Bind(CD_PRT)
	ViewColumn port();
	
	@Bind(VA_PTH)
	ViewColumn path();
	
	@Bind(VA_QRY)
	ViewColumn query();
	
	@Bind(VA_CNT_TYP)
	ViewColumn media();
	
	@Bind(VA_ATH_SCH)
	ViewColumn auth();
	
	@Bind(CD_STT)
	ViewColumn status();
	
	@Bind(VA_I_SZE)
	@Expose(identity = "size_in")
	ViewColumn sizeIn();
	
	@Bind(VA_O_SZE)
	@Expose(identity = "size_out")
	ViewColumn sizeOut();
	
	@Bind(VA_BODY_CONTENT)
	@Expose(identity = "body_content")
	ViewColumn bodyContent();
	
	@Bind(VA_I_CNT_ENC)
	@Expose(identity = "content_encoding_in")
	ViewColumn contentEncodingIn();
	
	@Bind(VA_O_CNT_ENC)
	@Expose(identity = "content_encoding_out")
	ViewColumn contentEncodingOut();
	
	@Bind(DH_STR)
	ViewColumn start();
	
	@Bind(DH_END)
	ViewColumn end();
	
	@Bind(VA_THR)
	ViewColumn thread();
	
	@Bind(VA_USR)
	ViewColumn user();
	
	@Bind(VA_LNK)
	ViewColumn linked();
	
	@Bind(CD_PRN_SES)
	ViewColumn parent();
	
	@Bind(CD_INS)
	@Expose(identity = "instance_env")
	ViewColumn instanceEnv();
	
	default JoinGroup instance() {
		var instance = getInstance().getStore(InspectStore.class).instance();
		return joins(innerJoin(instance.getView(), instanceEnv().eq(instance.id())));
	}
	
	default JoinGroup exception() {
		var exception = getInstance().getStore(InspectStore.class).exception();
		return joins(leftJoin(exception.getView(), instanceEnv().eq(exception.parent())));
	}
	
	default Column elapsedTime() {
		return end().minus(start()).epoch();
	}
	
	@Expose(identity = "error_type")
    default Column errorTypeExpressions() {
    	var exception = getInstance().getStore(InspectStore.class).exception();
        return status().toCase()
                .when(eq(0), exception.errType())
                .when(ge(200).and(lt(400)), null)
                .when(ge(400).and(lt(500)), "ClientError")
                .orElse("ServerError");
    }
	
}
