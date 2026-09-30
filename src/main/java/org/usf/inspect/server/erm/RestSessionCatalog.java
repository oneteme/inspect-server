package org.usf.inspect.server.erm;

import org.usf.jquery.core.Column;
import org.usf.jquery.core.JoinGroup;
import org.usf.jquery.core.ViewColumn;
import org.usf.jquery.mvc.Bind;
import org.usf.jquery.mvc.DatasetCatalog;
import org.usf.jquery.mvc.Expose;
import org.usf.jquery.mvc.Typed;

import static org.usf.inspect.server.config.constant.FieldConstant.*;
import static org.usf.jquery.core.JDBCType.UUID;
import static org.usf.jquery.core.Join.innerJoin;
import static org.usf.jquery.core.JoinGroup.joins;
import static org.usf.jquery.core.Predicate.*;

public interface RestSessionCatalog extends SessionCatalog {

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
	
	@Bind(VA_I_CNT_ENC)
	@Expose(identity = "content_encoding_in")
	ViewColumn contentEncodingIn();
	
	@Bind(VA_O_CNT_ENC)
	@Expose(identity = "content_encoding_out")
	ViewColumn contentEncodingOut();
	
	@Bind(VA_NAM)
	@Expose(identity = "api_name")
	ViewColumn apiName();
	
	@Bind(VA_USR_AGT)
	@Expose(identity = "user_agt")
	ViewColumn userAgt();
	
	@Bind(VA_CCH_CTR)
	@Expose(identity = "cache_control")
	ViewColumn cacheControl();
	
	@Bind(VA_LNK)
	ViewColumn linked();

	@Bind("va_fwd_add")
	@Expose(identity = "intermediate_nodes")
	ViewColumn intermediateNodes();

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
