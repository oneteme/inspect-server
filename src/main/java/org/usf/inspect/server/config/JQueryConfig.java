package org.usf.inspect.server.config;

import static org.usf.jquery.mvc.StoreManager.getInstance;

import javax.sql.DataSource;

import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.usf.inspect.server.erm.InspectStore;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class JQueryConfig {

    private final DataSource ds;
    
    @EventListener(ApplicationStartedEvent.class)
    void onReady() {
    	getInstance().register(InspectStore.class, ds);
    }
}
