package org.usf.inspect.server.erm;

import static org.usf.inspect.server.config.constant.FieldConstant.*;
import static org.usf.jquery.core.Join.innerJoin;
import static org.usf.jquery.core.JoinGroup.joins;
import static org.usf.jquery.core.Predicate.ge;
import static org.usf.jquery.core.Predicate.lt;
import static org.usf.jquery.mvc.StoreManager.getInstance;

import org.usf.jquery.core.*;
import org.usf.jquery.mvc.Bind;
import org.usf.jquery.mvc.DatasetCatalog;
import org.usf.jquery.mvc.Expose;

public interface RestSessionCatalog extends DatasetCatalog {

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
	
//	@Bind(CD_STT)
//	ViewColumn status();
	
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
	ViewColumn apiName();
	
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

	@Bind(CD_STT)
	ViewColumn status();

	@Bind(CD_INS)
	@Expose(identity = "instance_env")
	ViewColumn instanceEnv();

	default JoinGroup instance() {
		var instance = getInstance().getStore(InspectStore.class).instance();
		return joins(innerJoin(instance.getView(), instanceEnv().eq(instance.id())));
	}

	default JoinGroup databaseRequest() {
		var databaseRequest = getInstance().getStore(InspectStore.class).databaseRequest();
		return joins(innerJoin(databaseRequest.getView(), id().eq(databaseRequest.parent())));
	}

	default JoinGroup ftpRequest() {
		var ftpRequest = getInstance().getStore(InspectStore.class).ftpRequest();
		return joins(innerJoin(ftpRequest.getView(), id().eq(ftpRequest.parent())));
	}

	default JoinGroup smtpRequest() {
		var smtpRequest = getInstance().getStore(InspectStore.class).smtpRequest();
		return joins(innerJoin(smtpRequest.getView(), id().eq(smtpRequest.parent())));
	}

	default JoinGroup ldapRequest() {
		var ldapRequest = getInstance().getStore(InspectStore.class).ldapRequest();
		return joins(innerJoin(ldapRequest.getView(), id().eq(ldapRequest.parent())));
	}

	@Expose(identity = "error_type_session")
    default Column errorTypeExpressionsSession() {
        return status().toCase()
                .when(ge(200).and(lt(400)), null)
                .when(ge(400).and(lt(500)), "ClientError")
                .orElse(errType());
    }
	
    default Column countStatusByType(ViewColumn status, Predicate op) {
        return status.toCase().when(op, status).orElse(null).count();
    }
    
    @Expose(identity = "count_error_server")
    default Column countErrorServerStatus() {
    	return countStatusByType(status(), ge(500));
    }
    
    @Expose(identity = "count_error_client")
    default Column countClientErrorStatus() {
    	return countStatusByType(status(), ge(400).and(lt(500)));
    }
}
