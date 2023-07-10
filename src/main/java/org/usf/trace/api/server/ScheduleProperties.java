package org.usf.trace.api.server;

import lombok.Getter;
import lombok.Setter;

import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.concurrent.TimeUnit;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "trace")
public class ScheduleProperties {
	
    private int delay;
	private TimeUnit unit = SECONDS;

	public void setUnit(String unit){
		this.unit = TimeUnit.valueOf(unit.toLowerCase());
	}

}
