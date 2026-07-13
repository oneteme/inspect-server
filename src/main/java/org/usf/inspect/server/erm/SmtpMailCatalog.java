package org.usf.inspect.server.erm;

import static org.usf.inspect.server.config.constant.FieldConstant.CD_SMTP_RQT;
import static org.usf.inspect.server.config.constant.FieldConstant.VA_CNT_TYP;
import static org.usf.inspect.server.config.constant.FieldConstant.VA_FRM;
import static org.usf.inspect.server.config.constant.FieldConstant.VA_RCP;
import static org.usf.inspect.server.config.constant.FieldConstant.VA_RPL;
import static org.usf.inspect.server.config.constant.FieldConstant.VA_SBJ;
import static org.usf.inspect.server.config.constant.FieldConstant.VA_SZE;
import static org.usf.jquery.core.JDBCType.UUID;

import org.usf.jquery.core.ViewColumn;
import org.usf.jquery.mvc.Bind;
import org.usf.jquery.mvc.DatasetCatalog;
import org.usf.jquery.mvc.Expose;
import org.usf.jquery.mvc.Typed;

public interface SmtpMailCatalog extends DatasetCatalog {

	@Bind(VA_SBJ)
	ViewColumn subject();
	
	@Bind(VA_FRM)
	ViewColumn from();
	
	@Bind(VA_RCP)
	ViewColumn recipients();
	
	@Bind(VA_CNT_TYP)
	ViewColumn media();
	
	@Bind(VA_RPL)
	@Expose(identity = "reply_to")
	ViewColumn replyTo();
	
	@Bind(VA_SZE)
	ViewColumn size();
	
	@Bind(CD_SMTP_RQT)
	@Typed(UUID)
	ViewColumn parent();
}
