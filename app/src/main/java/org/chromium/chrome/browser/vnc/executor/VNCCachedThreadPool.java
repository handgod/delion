package org.chromium.chrome.browser.vnc.executor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class VNCCachedThreadPool{
    private static ExecutorService executorService = Executors.newCachedThreadPool();
    private VNCCachedThreadPool(){}
    public static ExecutorService getInstance(){
        return executorService;
    }
}
