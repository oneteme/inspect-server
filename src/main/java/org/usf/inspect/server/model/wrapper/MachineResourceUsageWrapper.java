package org.usf.inspect.server.model.wrapper;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.experimental.Delegate;
import org.usf.inspect.core.MachineResourceUsage;
import org.usf.inspect.server.model.InstanceEventTrace;

import java.time.Instant;

public class MachineResourceUsageWrapper extends InstanceEventTrace {
    @Delegate
    @JsonIgnore
    private final MachineResourceUsage machineResourceUsage;

    MachineResourceUsageWrapper(Instant instant, int usedHeap, int commitedHeap, int usedMeta, int commitedMeta, int usedDiskSpace) {
        this.machineResourceUsage = new MachineResourceUsage(instant, usedHeap, commitedHeap, usedMeta, commitedMeta, usedDiskSpace);
    }
}
