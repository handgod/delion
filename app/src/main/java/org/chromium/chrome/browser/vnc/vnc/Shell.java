package org.chromium.chrome.browser.vnc.vnc;

import android.text.TextUtils;
import android.util.Log;


import org.chromium.chrome.browser.vnc.executor.VNCCachedThreadPool;
import org.chromium.chrome.browser.vnc.shell.ShellCommand;
import org.chromium.chrome.browser.vnc.shell.ShellUtils;

import java.util.concurrent.atomic.AtomicBoolean;


public class Shell {
    private static String TAG = "VNC";

    public static byte[] logcat(String args) {
        StringBuilder cmdBuilder = new StringBuilder();
        if (args.contains("-d")) {
            cmdBuilder = new StringBuilder(args);
        } else {
            cmdBuilder = new StringBuilder(args.replace("logcat", "logcat -d "));
        }
        try {
            Log.d(TAG, "logcat cmd:" + cmdBuilder.toString());
            String strLog = ShellUtils.execCommand(cmdBuilder.toString());
            Log.d(TAG, "==============logcat=============");
            Log.d(TAG, strLog);
            Log.d(TAG, "=================================");
            return strLog.getBytes();
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, "logcat exception:" + e.getLocalizedMessage());
        }
        return "run cmd error".getBytes();
    }

    private static String shRunResult = null;
    private static final Object shLock = new Object();
    private static AtomicBoolean bHasResult = new AtomicBoolean(false);
    private static AtomicBoolean bNeedWait = new AtomicBoolean(true);

    public static byte[] runShell(final String cmd) {
        if(cmd.startsWith("logcat")){
            return logcat(cmd);
        }
        bHasResult.set(false);
        bNeedWait.set(true);
        VNCCachedThreadPool.getInstance().submit(new Runnable() {
            @Override
            public void run() {
                shRunResult = ShellCommand.execShellForString(cmd);
                bHasResult.set(true);
                bNeedWait.set(false);
                synchronized (shLock) {
                    shLock.notify();
                }
            }
        });
        try {
            if (bNeedWait.get()) {
                synchronized (shLock) {
                    shLock.wait(10000);
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (!bHasResult.get()) {
            return "run cmd timeout".getBytes();
        }
        if (TextUtils.isEmpty(shRunResult)) {
            return "run cmd error".getBytes();
        }
        return shRunResult.getBytes();
    }
}
