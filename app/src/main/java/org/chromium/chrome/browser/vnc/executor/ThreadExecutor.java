package org.chromium.chrome.browser.vnc.executor;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ThreadExecutor extends ThreadPoolExecutor {
    private static int corePoolSize = 20;
    private static int maximumPoolSize = Integer.MAX_VALUE;

    private static ThreadExecutor instance = new ThreadExecutor();

    private ThreadExecutor(){
        super(corePoolSize, maximumPoolSize, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), new ThreadPoolExecutor.AbortPolicy());
        allowCoreThreadTimeOut(true);
    }

    public static ThreadExecutor getInstance(){
        return instance;
    }
}
