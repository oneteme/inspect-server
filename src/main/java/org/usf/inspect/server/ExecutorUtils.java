package org.usf.inspect.server;

import org.usf.inspect.core.ExecutorServiceWrapper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ExecutorUtils {
    public static ExecutorService virtualThreadExecutor(String name, int size) {
        return ExecutorServiceWrapper.wrap(new ThreadPoolExecutor(size, size, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(), Thread.ofVirtual().name(name + "-", 0).factory()));
    }
}
