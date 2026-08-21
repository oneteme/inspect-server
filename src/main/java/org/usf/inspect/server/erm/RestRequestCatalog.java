package org.usf.inspect.server.erm;

import static org.usf.inspect.core.RequestMask.REST;
import static org.usf.inspect.server.config.constant.FieldConstant.CD_PRN_SES;
import static org.usf.inspect.server.config.constant.FieldConstant.CD_STT;
import static org.usf.inspect.server.config.constant.FieldConstant.ID_RST_RQT;
import static org.usf.inspect.server.config.constant.FieldConstant.VA_ATH_SCH;
import static org.usf.inspect.server.config.constant.FieldConstant.VA_BODY_CONTENT;
import static org.usf.inspect.server.config.constant.FieldConstant.VA_CNT_TYP;
import static org.usf.inspect.server.config.constant.FieldConstant.VA_I_CNT_ENC;
import static org.usf.inspect.server.config.constant.FieldConstant.VA_I_SZE;
import static org.usf.inspect.server.config.constant.FieldConstant.VA_LNK;
import static org.usf.inspect.server.config.constant.FieldConstant.VA_MTH;
import static org.usf.inspect.server.config.constant.FieldConstant.VA_O_CNT_ENC;
import static org.usf.inspect.server.config.constant.FieldConstant.VA_O_SZE;
import static org.usf.inspect.server.config.constant.FieldConstant.VA_PCL;
import static org.usf.inspect.server.config.constant.FieldConstant.VA_PTH;
import static org.usf.inspect.server.config.constant.FieldConstant.VA_QRY;
import static org.usf.jquery.core.JDBCType.UUID;
import static org.usf.jquery.core.Predicate.eq;
import static org.usf.jquery.core.Predicate.ge;
import static org.usf.jquery.core.Predicate.lt;
import static org.usf.jquery.mvc.StoreManager.getInstance;

import org.usf.inspect.core.RequestMask;
import org.usf.jquery.core.Column;
import org.usf.jquery.core.ViewColumn;
import org.usf.jquery.mvc.Bind;
import org.usf.jquery.mvc.Expose;
import org.usf.jquery.mvc.Typed;

public interface RestRequestCatalog extends RequestCatalog {

	@Bind(ID_RST_RQT)
	@Typed(UUID)
	ViewColumn id();

	@Bind(VA_MTH)
	ViewColumn method();
	
	@Bind(VA_PCL)
	ViewColumn protocol();
	
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
	
	@Bind(VA_LNK)
	ViewColumn linked();
	
	@Bind(CD_PRN_SES)
	@Typed(UUID)
	ViewColumn parent();
	
	@Override
	default RequestMask getRequestType() {
		return REST;
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

	@Expose(identity = "count_error")
	default Column countError() {
		return status().toCase().when(eq(0).or(ge(400)), true).compose().count();
    }

	@Expose(identity = "status_tranche")
	default Column statusTranche() {
		return status().toCase()
				.when(eq(0), "1")
				.when(ge(100).and(lt(200)), "2")
				.when(ge(200).and(lt(300)), "3")
				.when(ge(300).and(lt(400)), "4")
				.when(ge(400).and(lt(500)), "5")
				.when(ge(500), "6").compose();
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
