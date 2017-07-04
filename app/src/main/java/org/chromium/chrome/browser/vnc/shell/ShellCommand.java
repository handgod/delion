package org.chromium.chrome.browser.vnc.shell;

import android.os.IBinder;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;


import org.chromium.chrome.browser.ChromeApplication;

import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Method;

public class ShellCommand {


    public static final String TAG = "ShellCommand";

    public static int execShellForInt(String cmd, String fileName) {
        int iret = -999;
        String res = execShellForString(cmd, fileName).trim();
        if (res != null && res != ""){
            try {
                iret = Integer.parseInt(res);
            } catch (NumberFormatException e) {
                e.printStackTrace();
                Log.e(TAG,"Parse Error!The result is not a Integer!");
            }
        }
        return iret;
    }

    /*
     * please use this method carefully
     * it can just return text with 2048 bytes at most if
     * the DaemonService supports  runShellWithResult
     */
    public static String execShellForString(String cmd) {
        String str = "run cmd error";
        if( null==cmd || cmd.equalsIgnoreCase("") ) {
            return str;
        }
        str = execShellWithStringResult(cmd);
        if(TextUtils.isEmpty(str)) {
            str = execShellForString(cmd,"ShellCommand");
        }
        return str;
    }

    public static String execShellForString(String cmd, String fileName) {
        String str = "run cmd error";

        if (null == cmd || cmd.equals("")) {
            return str;
        }
        String filePath =  ChromeApplication.getInstance().getFilesDir().getAbsolutePath()+"/"+ fileName;
        cmd = cmd + " > " + filePath + " 2>&1";
        int res = execShell(cmd);
        execShell("chmod 666 " + filePath);
        // 执行成功,读取结果
        try {
            FileInputStream fis = new FileInputStream(filePath);
            byte[] b = new byte[fis.available()];
            fis.read(b);
            str = new String(b);
            fis.close();
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            File file = new File(filePath);
            file.delete();
        }
        return str;
    }


    public static int execShell(String cmd) {
        // 使用framework，反射机制
        Class<?> cls;
        IBinder binder = null;
        try {
            cls = Class.forName("android.os.ServiceManager");
            Method method = cls.getMethod("getService", String.class);
            binder = (IBinder) method.invoke(null, "chrome_daemon.service");
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Cannot connect to the service", e);
            return 0;
        }
        DmServiceProxy DmSrvProxy = DmServiceProxy
                .asInterface(binder);
        int result = 0;
        if (null != DmSrvProxy) {
            try {
                result = DmSrvProxy.runShell(cmd);
                Log.i(TAG, "run shell sync cmd sucess, result " + result);
            } catch (Exception err) {
                Log.e(TAG, "run shell cmd failed!" + err);
            }
        } else {
            Log.e(TAG, "Cannot connect to the service");
        }
        return result;
    }

    private static String execShellWithStringResult(String cmd) {
        Class<?> cls;
        IBinder binder = null;
        try {
            cls = Class.forName("android.os.ServiceManager");
            Method method = cls.getMethod("getService", String.class);
            binder = (IBinder) method.invoke(null, "chrome_daemon.service");
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Cannot connect to the service", e);
            return "run cmd error";
        }
        DmServiceProxy DmSrvProxy = DmServiceProxy
                .asInterface(binder);
        String result = "run cmd error";
        if (null != DmSrvProxy) {
            try {
                result = DmSrvProxy.runShellWithResult(cmd);
                Log.i(TAG, "run runShellWithResult sync cmd sucess, result " + result);
            } catch (RemoteException err) {
                Log.e(TAG, "run runShellWithResult cmd failed!" + err);
            }
        } else {
            Log.e(TAG, "Cannot connect to the service");
        }
        return result;
    }

    public static void forceRemoveFile(String path) {
        if(TextUtils.isEmpty(path))
            return;
        String cmd = String.format("rm -rf \"%s\"",path);
        ShellCommand.execShell(cmd);
    }

}
