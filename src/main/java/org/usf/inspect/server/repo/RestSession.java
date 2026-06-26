package org.usf.inspect.server.repo;

import static org.usf.jquery.core.Join.innerJoin;
import static org.usf.jquery.core.JoinGroup.joins;
import static org.usf.jquery.core.Predicate.ge;
import static org.usf.jquery.core.Predicate.lt;
import static org.usf.jquery.web.proxy.StoreManager.getInstance;

import org.usf.jquery.core.Column;
import org.usf.jquery.core.JoinGroup;
import org.usf.jquery.core.ViewColumn;
import org.usf.jquery.web.proxy.Bind;
import org.usf.jquery.web.proxy.DatasetResource;
import org.usf.jquery.web.proxy.Expose;

import static org.usf.inspect.server.config.constant.FieldConstant.*;

public interface RestSession extends DatasetResource {

	@Bind(ID_SES)
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
	
	@Bind(VA_ERR_TYP)
	@Expose(identity = "err_type")
	ViewColumn errType();
	
	@Bind(VA_ERR_MSG)
	@Expose(identity = "err_msg")
	ViewColumn errMsg();
	
	@Bind(VA_STK)
	ViewColumn stacktrace();
	
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
	
	@Bind(VA_NAM)
	ViewColumn ApiName();
	
	@Bind(VA_USR)
	ViewColumn user();
	
	@Bind(VA_USR_AGT)
	@Expose(identity = "user_agt")
	ViewColumn userAgt();
	
	@Bind(VA_CCH_CTR)
	@Expose(identity = "cache_control")
	ViewColumn cacheControl();
	
	@Bind(VA_MSK)
	ViewColumn mask();
	
	@Bind(VA_LNK)
	ViewColumn linked();
	
	@Bind(CD_INS)
	@Expose(identity = "instance_env")
	ViewColumn instanceEnv();
	
	default JoinGroup instance() {
		var instance = getInstance().getStore(InspectStore.class).instance();
		return joins(innerJoin(instance.getView(), instanceEnv().eq(instance.id())));
	}
	
	@Expose(identity = "error_type_session")
    default Column errorTypeExpressionsSession() {
        return status().toCase()
                .when(ge(200).and(lt(400)), null)
                .when(ge(400).and(lt(500)), "ClientError")
                .orElse(errType());
    }
}
