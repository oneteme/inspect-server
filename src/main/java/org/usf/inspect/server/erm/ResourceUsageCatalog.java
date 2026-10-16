package org.usf.inspect.server.erm;

import static org.usf.inspect.server.config.constant.FieldConstant.*;
import static org.usf.jquery.core.JDBCType.UUID;

import org.usf.jquery.core.ViewColumn;
import org.usf.jquery.mvc.Bind;
import org.usf.jquery.mvc.DatasetCatalog;
import org.usf.jquery.mvc.Expose;
import org.usf.jquery.mvc.Typed;

public interface ResourceUsageCatalog extends DatasetCatalog<InspectStore> {
	
	@Bind(VA_USED_HEP)
	@Expose(identity = "used_heap")
	ViewColumn usedHeap();
	
	@Bind(VA_COMMITED_HEP)
	@Expose(identity = "commited_heap")
	ViewColumn commitedHeap();
	
	@Bind(VA_USED_DISK_SPACE)
	@Expose(identity = "used_disk_space")
	ViewColumn usedDiskSpace();

	@Bind(NB_ACTIVE_THREAD)
	@Expose(identity ="nb_active_thread")
	ViewColumn nbActiveThread();

	@Bind(NB_START_THREAD)
	@Expose(identity = "nb_start_thread")
	ViewColumn nbStartThread();

	@Bind(VA_CPU_USAGE)
	@Expose(identity = "cpu_usage")
	ViewColumn cpuUsage();

	@Bind(DH_STR)
	ViewColumn start();
	
	@Bind(CD_INS)
	@Expose(identity = "instance_env")
	@Typed(UUID)
	ViewColumn instanceEnv();
}
