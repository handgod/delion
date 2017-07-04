package org.chromium.chrome.browser.vnc.vnc;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

/**
 * Created by liangjun on 2017/3/24.
 */

public class RemoteConnectService extends Service {
    private static final String TAG = "RemoteConnectService";
    public static final String MSG_REQUEST_OPCODE = "req_opcode";
    public static final String MSG_CONNECT = "connect";
    public static final String MSG_DISCONNECT = "disconnect";
    public void onCreate() {
        super.onCreate();
        clientProxy = new RemoteClientProxy();
    }
    private RemoteClientProxy clientProxy;
    public int onStartCommand(Intent intent, int flags, int startId) {

        if(intent.getAction().equals(MSG_REQUEST_OPCODE)){
            clientProxy.getOpcode();
        }else if(intent.getAction().equals(MSG_CONNECT)){
            try {
                clientProxy.startConnect();
                Log.d(TAG, "clientProxy.startConnect success");
            }catch (Exception e){
                Log.d(TAG, "clientProxy.startConnect failed:" + e.toString());
            }
        }else if(intent.getAction().equals(MSG_DISCONNECT)){
            if(clientProxy != null) {
                clientProxy.disconnect();
            }
        }
        return START_NOT_STICKY;
    }
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void onDestroy() {
        super.onCreate();
    }

    public static void requestOpcode(Context context){
        Intent connect = new Intent(context, RemoteConnectService.class);
        connect.setAction(RemoteConnectService.MSG_REQUEST_OPCODE);
        context.startService(connect);
    }
    public static void startStunConnection(Context context){
        Intent connect = new Intent(context, RemoteConnectService.class);
        connect.setAction(RemoteConnectService.MSG_CONNECT);
        context.startService(connect);
    }

    public static void closeStunConnection(Context context){
        Intent connect = new Intent(context, RemoteConnectService.class);
        connect.setAction(RemoteConnectService.MSG_DISCONNECT);
        context.startService(connect);
    }
}
