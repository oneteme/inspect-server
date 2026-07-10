package org.usf.inspect.server.repo;

import org.usf.jquery.core.Column;
import org.usf.jquery.core.Join;
import org.usf.jquery.core.ViewColumn;
import org.usf.jquery.mvc.Bind;
import org.usf.jquery.mvc.Expose;

import static org.usf.inspect.server.config.constant.FieldConstant.*;
import static org.usf.jquery.core.Join.leftJoin;
import static org.usf.jquery.core.Predicate.*;
import static org.usf.jquery.mvc.StoreManager.getInstance;

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
	
	default Join exception() {
		var exception = getInstance().getStore(InspectStore.class).exception();
		return leftJoin(exception.getView(), id().eq(exception.parent()).and(exception.type().eq("REST")));
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
