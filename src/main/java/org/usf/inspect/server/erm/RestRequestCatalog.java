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
import static org.usf.jquery.core.Predicate.eq;
import static org.usf.jquery.core.Predicate.ge;
import static org.usf.jquery.core.Predicate.lt;
import static org.usf.jquery.mvc.StoreManager.getInstance;

import org.usf.inspect.core.RequestMask;
import org.usf.jquery.core.Column;
import org.usf.jquery.core.ViewColumn;
import org.usf.jquery.mvc.Bind;
import org.usf.jquery.mvc.Expose;

public interface RestRequestCatalog extends RequestCatalog {

	@Bind(ID_RST_RQT)
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
	
}
