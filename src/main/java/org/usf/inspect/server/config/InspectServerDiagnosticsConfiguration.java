package org.usf.inspect.server.config;

import static ch.qos.logback.classic.Level.WARN;
import static java.time.Instant.ofEpochMilli;
import static java.util.Objects.nonNull;
import static org.slf4j.LoggerFactory.getILoggerFactory;
import static org.slf4j.LoggerFactory.getLogger;
import static org.usf.inspect.server.service.TraceBatchDispatcherHub.serverHub;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.usf.inspect.core.ReportEvent;
import org.usf.inspect.core.StackTraceRow;
import org.usf.inspect.server.InspectApplication;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ch.qos.logback.core.AppenderBase;
import lombok.RequiredArgsConstructor;

/**
 * 
 * @author u$f
 *
 */
@Configuration
@ConditionalOnProperty(name = "inspect.server.diagnostics.enabled", havingValue = "true")
public class InspectServerDiagnosticsConfiguration {
	
    public InspectServerDiagnosticsConfiguration() {
        var app = new InspectLogAppender(WARN);
        app.setContext((LoggerContext) getILoggerFactory());
        app.setName("INSPECT_REPORTER");
        app.start();
        ((Logger) getLogger(InspectApplication.class.getPackage().getName())).addAppender(app);
    }

	@RequiredArgsConstructor
	public final class InspectLogAppender extends AppenderBase<ILoggingEvent> {
		
		private final Level level;

	    @Override
	    protected void append(ILoggingEvent event) {
	        if(event.getLevel().isGreaterOrEqual(level)) {
	        	try {
					var lvl = event.getLevel().toString();
					var tms = ofEpochMilli(event.getTimeStamp());
					String exp = null;
					StackTraceRow[] stc = null;
					if(nonNull(event.getThrowableProxy())) {
						exp = event.getThrowableProxy().getClassName() + ": " + event.getThrowableProxy().getMessage();
						stc = toStackTraceRow(event.getThrowableProxy().getStackTraceElementProxyArray());
					}
					var rpr = new ReportEvent(tms, event.getThreadName(), lvl, event.getFormattedMessage(), exp, stc);
					serverHub().emitTrace(rpr);
	        	}
	        	catch (Exception e) {
	        		//do not log, avoid loop
	        	}
	        }
	    }
	}
	
	static StackTraceRow[] toStackTraceRow(StackTraceElementProxy[] stack) {
		if(nonNull(stack) && stack.length > 0) {
			var stc = new StackTraceRow[stack.length];
			for(var i=0; i<stack.length; i++) {
				var e = stack[i].getStackTraceElement();
				stc[i] = new StackTraceRow(e.getClassName(), e.getMethodName(), e.getLineNumber());
			}
			return stc;
		}
		return null;
	}
}
