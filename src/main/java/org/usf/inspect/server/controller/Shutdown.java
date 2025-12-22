package org.usf.inspect.server.controller;

import java.time.Instant;
import java.util.concurrent.Callable;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.usf.inspect.core.Helper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
public class Shutdown {
	
	private final ApplicationContext context;
    
    @GetMapping("/shutdown/1")
    public void shutdownContext1() {
        ((ConfigurableApplicationContext) context).close();
    }

    @GetMapping("/shutdown/2")
    public void shutdownContext2() {
        System.exit(1);
    }
    
    @GetMapping("/shutdown/3")
    public void shutdownContext3() {
    	Runtime.getRuntime().exit(0);
    }

    @GetMapping("call")
    public Callable<String> getInstance(@RequestParam(name="value", required = false) String value) {
    	if(value.equals("1")) {
    		throw new RuntimeException(value);
    	}
        return ()->{
        	log.debug("LOG");
        	if(value.equals("2")) {
        		throw new RuntimeException(value);
        	}
        	return "u$f " + value;
        };
    }

    @GetMapping("async")
    public Callable<String> asyncTest() throws InterruptedException {
		Thread.sleep(5000);
		System.err.println(Instant.now() + " " + Helper.threadName() + " before return callable");
    	return ()->{
    		Thread.sleep(3000);
    		System.err.println(Instant.now() + " " + Helper.threadName() + " callable execution");
    		return "async";
    	};
    }
}
