package org.usf.inspect.server.erm;

import org.usf.jquery.core.Column;
import org.usf.jquery.core.ViewColumn;
import org.usf.jquery.mvc.Bind;
import org.usf.jquery.mvc.Expose;
import org.usf.jquery.mvc.Typed;

import static org.usf.inspect.server.config.constant.FieldConstant.*;
import static org.usf.jquery.core.JDBCType.UUID;
import static org.usf.jquery.core.Predicate.*;
import static org.usf.jquery.mvc.StoreManager.getInstance;

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
