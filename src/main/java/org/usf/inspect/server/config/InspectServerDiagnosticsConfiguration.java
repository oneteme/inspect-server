package org.usf.inspect.server.config;

import static org.usf.inspect.logging.InspectLogReporter.configureLogger;
import static org.usf.inspect.server.service.TraceBatchDispatcherHub.INSPECT_ID;
import static org.usf.inspect.server.service.TraceBatchDispatcherHub.serverHub;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.usf.inspect.core.ReportEvent;
import org.usf.inspect.logging.InspectLogReporter;
import org.usf.inspect.server.InspectServerConfiguration;

import ch.qos.logback.classic.spi.ILoggingEvent;

/**
 * 
 * @author u$f
 *
 */
@Configuration
@ConditionalOnProperty(name = "inspect.server.reporting.enabled", havingValue = "true", matchIfMissing = true)
public class InspectServerDiagnosticsConfiguration {

    public InspectServerDiagnosticsConfiguration(InspectServerConfiguration config) {
    	var report = config.getReporting();
		configureLogger("org.usf.inspect.server", true, ()-> new InspectServerLogReporter(report.isStackTrace()));
    }

	public final class InspectServerLogReporter extends InspectLogReporter {
		
		public InspectServerLogReporter(boolean stackTrace) {
			super(serverHub(), stackTrace);
		}
		
		@Override
		protected ReportEvent createReportEvent(ILoggingEvent event) {
			var trc = super.createReportEvent(event);
			trc.setInstanceId(INSPECT_ID);
			return trc;
		}
	}
}
