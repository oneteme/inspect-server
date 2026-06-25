package org.usf.inspect.server.repo;

import static org.usf.inspect.server.config.constant.FieldConstant.*;

import org.usf.jquery.core.ViewColumn;
import org.usf.jquery.web.proxy.Bind;
import org.usf.jquery.web.proxy.DatasetResource;
import org.usf.jquery.web.proxy.Expose;

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
	@Expose(identity = "instance")
	ViewColumn instanceEnv();
}
