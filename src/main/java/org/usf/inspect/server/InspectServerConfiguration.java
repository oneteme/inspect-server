package org.usf.inspect.server;

import org.usf.inspect.core.InspectCollectorConfiguration;
import org.usf.inspect.core.MonitoringConfiguration;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
public final class InspectServerConfiguration extends InspectCollectorConfiguration {
   
    private PartitionProperties partition = new PartitionProperties();
    private PurgeProperties purge = new PurgeProperties();
    
    public InspectServerConfiguration() {
    	setEnabled(true); //server is always enabled
    }
    
    @Override
    public void setMonitoring(MonitoringConfiguration monitoring) {
    	throw new UnsupportedOperationException("Monitoring configuration is not supported in Inspect Server");
    }
    
//    @Override
//    public MonitoringConfiguration getMonitoring() {
//    	throw new UnsupportedOperationException("Monitoring configuration is not supported in Inspect Server");
//    }
    
    //TODO validate
}
