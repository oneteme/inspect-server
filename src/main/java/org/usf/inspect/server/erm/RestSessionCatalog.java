package org.usf.inspect.server.erm;

import static org.usf.inspect.server.config.constant.FieldConstant.*;
import static org.usf.jquery.core.JDBCType.UUID;
import static org.usf.jquery.core.Join.innerJoin;
import static org.usf.jquery.core.JoinGroup.joins;
import static org.usf.jquery.core.Predicate.*;
import static org.usf.jquery.mvc.StoreManager.getInstance;

import org.usf.jquery.core.*;
import org.usf.jquery.mvc.Bind;
import org.usf.jquery.mvc.DatasetCatalog;
import org.usf.jquery.mvc.Expose;
import org.usf.jquery.mvc.Typed;

public interface RestSessionCatalog extends DatasetCatalog<InspectStore> {

	@Bind(ID_SES)
	@Typed(UUID)
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
	@Expose(identity = "api_name")
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
	@Typed(UUID)
	ViewColumn instanceEnv();

	default JoinGroup instance() {
		var instance = getStore().instance();
		return joins(innerJoin(instance.getView(), instanceEnv().eq(instance.id())));
	}

	default JoinGroup restRequest() {
		var restRequest = getStore().restRequest();
		return joins(innerJoin(restRequest.getView(), id().eq(restRequest.parent())));
	}

	default JoinGroup databaseRequest() {
		var databaseRequest = getStore().databaseRequest();
		return joins(innerJoin(databaseRequest.getView(), id().eq(databaseRequest.parent())));
	}

	default JoinGroup ftpRequest() {
		var ftpRequest = getStore().ftpRequest();
		return joins(innerJoin(ftpRequest.getView(), id().eq(ftpRequest.parent())));
	}

	default JoinGroup smtpRequest() {
		var smtpRequest = getStore().smtpRequest();
		return joins(innerJoin(smtpRequest.getView(), id().eq(smtpRequest.parent())));
	}

	default JoinGroup ldapRequest() {
		var ldapRequest = getStore().ldapRequest();
		return joins(innerJoin(ldapRequest.getView(), id().eq(ldapRequest.parent())));
	}

	@Expose(identity = "elapsed_time")
	default Column elapsedTime() {
		return end().minus(start()).epoch();
	}

	@Expose(identity = "error_type_session")
    default Column errorTypeExpressionsSession() {
        return status().toCase()
                .when(ge(200).and(lt(400)), null)
                .when(ge(400).and(lt(500)), "ClientError")
                .orElse(errType());
    }
    
    @Expose(identity = "count_error_server")
    default Column countErrorServerStatus() {
		return status().toCase().when(ge(500), status()).compose().count();
    }
    
    @Expose(identity = "count_error_client")
    default Column countClientErrorStatus() {
		return status().toCase().when(ge(400).and(lt(500)), status()).compose().count();
    }

	@Expose(identity = "count_error")
	default Column countError() {
		return status().toCase().when(eq(0).or(ge(400)), status()).compose().count();
	}

	@Expose(identity = "performance_tranche")
	default Column performanceTranche1() {
		return elapsedTime().toCase()
				.when(lt(1), "1")
				.when(ge(1).and(lt(3)), "2")
               	.when(ge(3).and(lt(5)), "3")
                .when(ge(5).and(lt(10)), "4")
                .when(ge(10), "5").compose();
	}

	@Expose(identity = "performance_tranche2")
	default Column performanceTranche2() {
		return elapsedTime().toCase()
				.when(lt(5), "1")
				.when(ge(5).and(lt(10)), "2")
				.when(ge(10), "3").compose();
	}

	@Expose(identity = "size_in_tranche")
	default Column sizeInTranche() {
		return sizeIn().toCase()
				.when(lt(100), "1")
				.when(ge(100).and(lt(200)), "2")
				.when(ge(200).and(lt(300)), "3")
				.when(ge(300), "4").compose();
	}

	@Expose(identity = "size_out_tranche")
	default Column sizeOutTranche() {
		return sizeOut().toCase()
				.when(lt(100), "1")
				.when(ge(100).and(lt(200)), "2")
				.when(ge(200).and(lt(300)), "3")
				.when(ge(300), "4").compose();
	}

	@Expose(identity = "size_in_notnull")
	default Column sizeInNotNull() {
		return sizeIn().toCase().when(eq(-1), 0).orElse(sizeIn());
	}

	@Expose(identity = "size_out_notnull")
	default Column sizeOutNotNull() {
		return sizeOut().toCase().when(eq(-1), 0).orElse(sizeOut());
	}
}
