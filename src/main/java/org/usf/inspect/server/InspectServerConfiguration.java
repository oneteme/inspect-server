package org.usf.inspect.server;

import org.usf.inspect.InspectCollectorProperties;
import org.usf.inspect.MonitoringProperties;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
public final class InspectServerConfiguration extends InspectCollectorProperties {
   
    private PartitionProperties partition = new PartitionProperties();
    
    public InspectServerConfiguration() {
    	setEnabled(true); //server is always enabled
    	setDebugMode(false);
    }
    
    @Override
    public void setEnabled(boolean enabled) {
    	if(!enabled) {
			throw new UnsupportedOperationException("Inspect Server cannot be disabled");
		}
    }
    
    @Override
    public void setMonitoring(MonitoringProperties monitoring) {
    	throw new UnsupportedOperationException("Monitoring configuration is not supported in Inspect Server");
    }
}
