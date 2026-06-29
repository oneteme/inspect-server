package org.usf.inspect.server.repo;

import static org.usf.inspect.server.config.constant.FieldConstant.CD_INS;
import static org.usf.inspect.server.config.constant.FieldConstant.DH_STR;
import static org.usf.inspect.server.config.constant.FieldConstant.VA_COMMITED_HEP;
import static org.usf.inspect.server.config.constant.FieldConstant.VA_USED_DISK_SPACE;
import static org.usf.inspect.server.config.constant.FieldConstant.VA_USED_HEP;

import org.usf.jquery.core.ViewColumn;
import org.usf.jquery.mvc.Bind;
import org.usf.jquery.mvc.DatasetResource;
import org.usf.jquery.mvc.Expose;

public interface ResourceUsage extends DatasetResource {
	
	@Bind(VA_USED_HEP)
	ViewColumn usedHeap();
	
	@Bind(VA_COMMITED_HEP)
	ViewColumn commitedHeap();
	
	@Bind(VA_USED_DISK_SPACE)
	ViewColumn usedDiskSpace();
	
	@Bind(DH_STR)
	ViewColumn start();
	
	@Bind(CD_INS)
	@Expose(identity = "instance_env")
	ViewColumn instanceEnv();
}
