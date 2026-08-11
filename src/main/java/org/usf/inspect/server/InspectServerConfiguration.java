package org.usf.inspect.server;

import org.usf.inspect.core.InspectCollectorConfiguration;
import org.usf.inspect.core.MonitoringConfiguration;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Holds the server-specific configuration for the Inspect application.
 */
@Setter
@Getter
@ToString
public final class InspectServerConfiguration extends InspectCollectorConfiguration {
   
    private PartitionProperties partition = new PartitionProperties();
    
    /**
     * Creates a server configuration with the server feature enabled by default.
     */
    public InspectServerConfiguration() {
    	setEnabled(true); //server is always enabled
    }
    
    /**
     * Rejects monitoring configuration because it is not supported by the server module.
     *
     * @param monitoring the monitoring configuration to reject
     */
    @Override
    public void setMonitoring(MonitoringConfiguration monitoring) {
    	throw new UnsupportedOperationException("Monitoring configuration is not supported in Inspect Server");
    }
}
