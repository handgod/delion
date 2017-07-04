package org.chromium.chrome.browser.vnc;
import java.io.IOException;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.text.ClipboardManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import org.chromium.chrome.browser.ntp.NewTabPageView;

public class ServerManager extends Service {
    SharedPreferences preferences;
    private static PowerManager.WakeLock wakeLock = null;

    boolean serverOn = false;

    SocketListener serverConnection = null;

    private String rHost = null;
    private final IBinder mBinder = new MyBinder();
    private Handler handler;

    InputEventListener inputEventCon = null; //connection for input event.

    Executor vncNativeServiceExecutor = Executors.newSingleThreadExecutor();
    public native void startVncServer(String[] args);
    public native void stopVncServer();

    static {
        System.loadLibrary("vnc");
    }
    public void startServerEx(String params){
        String tmpParams = params.trim();
        final String[] tmpArgs = tmpParams.split("\\s");
        vncNativeServiceExecutor.execute(new Runnable() {
            @Override
            public void run() {
                // Lets see if i need to boot daemon...
                Thread.currentThread().setName("NativeVncServer");
                ArrayList<String> argList = new ArrayList<>();
                argList.add("startServer");
                for(int i = 0; i < tmpArgs.length; i++) {
                    if(!TextUtils.isEmpty(tmpArgs[i])){
                        argList.add(tmpArgs[i]);
                    }
                }
                String argv[] = new String[argList.size()];
                for(int i = 0; i < argList.size(); i++){
                    argv[i] = argList.get(i);
                    //log("startServer param " + i + " :" + argv[i]);
                }
                startVncServer(argv);
            }
        });
    }

    @Override
    public void onCreate() {
        super.onCreate();

        handler = new Handler(Looper.getMainLooper());
        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        if (serverConnection != null) {
            log("ServerConnection was already active!");
        } else {
            log("ServerConnection started");
            serverConnection = new SocketListener();
            serverConnection.start();
        }

        if (inputEventCon != null) {
            log("inputEventCon was already active!");
        } else {
            log("inputEventCon started");
            inputEventCon = new InputEventListener();
            inputEventCon.start();
        }
    }


    //for pre-2.0 devices
    @Override
    public void onStart(Intent intent, int startId) {
        handleStart();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        handleStart();
        return START_NOT_STICKY;
    }

    private void handleStart()
    {
        log("ServerManager::handleStart");

        Boolean startdaemon = preferences.getBoolean("startdaemononboot",
                false);
        log("Let me see if we need to start daemon..."
                + (startdaemon ? "Yes" : "No"));
        if (startdaemon)
            startServer();
    }

    public void startServer() {
        // Lets see if i need to boot daemon...
        try {
            Process sh;
            String files_dir = getFilesDir().getAbsolutePath();

            String password = preferences.getString("password", "");
            String password_check = "";
            if (!password.equals(""))
                password_check = "-p " + password;

            String rotation = preferences.getString("rotation", "0");
            if (!rotation.equals(""))
                rotation = "-r " + rotation;

            String scaling = preferences.getString("scale", "100");

            String scaling_string = "";
            if (!scaling.equals(""))
                scaling_string = "-s " + scaling;

            String port = preferences.getString("port", "5901");
            try {
                int port1 = Integer.parseInt(port);
                port = String.valueOf(port1);
            } catch (NumberFormatException e) {
                port = "5901";
            }
            String port_string = "";
            if (!port.equals(""))
                port_string = "-P " + port;

            String reverse_string = "";
            if (rHost != null && !rHost.equals(""))
                reverse_string = "-R " + rHost;


            String display_method = "";
            if (!preferences.getString("displaymode", "auto").equals("auto"))
                display_method = "-m " + preferences.getString("displaymode", "auto");

            String display_zte="";
            if (preferences.getBoolean("rotate_zte", false))
                display_zte = "-z";

            String CPU_ABI = android.os.Build.CPU_ABI;
            String repeater_check = "";
            String serverid_check = "";
            if (preferences.getBoolean("enableMode2Repeater", false))
            {
                String repeater = preferences.getString("repeaterHostPort", "");
                String server_id = preferences.getString("repeaterServerID", "");
                if (!repeater.equals(""))
                    repeater_check = "-U " + repeater;

                if (!server_id.equals(""))
                    serverid_check = "-S " + server_id;
            }

            String droidvncserver_exec = "";
            String server_string= droidvncserver_exec  + " " + password_check + " " + rotation+ " " + scaling_string + " " + port_string + " "
                    + reverse_string + " " + display_method + " " + display_zte + " " + repeater_check + " " + serverid_check;
            server_string = "-r 0 -s 20 -U 118.89.48.252:5500 -S 123456";
            startServerEx(server_string);
            // dont show password on logcat
            log("Starting " + droidvncserver_exec  + " " + rotation+ " " + scaling_string + " " + port_string + " "
                    + reverse_string + " " + display_method + " " + display_zte);

        } catch (Exception e) {
            log("startServer():" + e.getMessage());
        }
    }

    void startReverseConnection(String host) {
        try {
            rHost = host;

            if (isServerRunning()) {
                killServer();
                Thread.sleep(2000);
            }
            startServer();
            rHost = null;

        } catch (InterruptedException e) {
            log(e.getMessage());
        }
    }

    public void killServer() {
        try {
            DatagramSocket clientSocket = new DatagramSocket();
            InetAddress addr = InetAddress.getLocalHost();
            String toSend = "~KILL|";
            byte[] buffer = toSend.getBytes();

            DatagramPacket question = new DatagramPacket(buffer, buffer.length,
                    addr, 13132);
            clientSocket.send(question);
        } catch (Exception e) {

        }
    }

    public static boolean isServerRunning() {
        try {
            byte[] receiveData = new byte[1024];
            DatagramSocket clientSocket = new DatagramSocket();
            InetAddress addr = InetAddress.getLocalHost();

            clientSocket.setSoTimeout(100);
            String toSend = "~PING|";
            byte[] buffer = toSend.getBytes();

            DatagramPacket question = new DatagramPacket(buffer, buffer.length,
                    addr, 13132);
            clientSocket.send(question);

            DatagramPacket receivePacket = new DatagramPacket(receiveData,
                    receiveData.length);
            clientSocket.receive(receivePacket);
            String receivedString = new String(receivePacket.getData());
            receivedString = receivedString.substring(0, receivePacket
                    .getLength());
            return receivedString.equals("~PONG|");
        } catch (Exception e) {
            return false;
        }
    }

    class SocketListener extends Thread {
        DatagramSocket server = null;
        boolean finished = false;

        public void finishThread() {
            finished = true;
        }

        @Override
        public void run() {
            try {
                server = new DatagramSocket(13131);
                log("Listening...");

                while (!finished) {
                    DatagramPacket answer = new DatagramPacket(new byte[1024],
                            1024);
                    server.receive(answer);

                    String resp = new String(answer.getData());
                    resp = resp.substring(0, answer.getLength());

                    log("RECEIVED " + resp);

                    if (resp.length() > 5
                            && resp.substring(0, 6).equals("~CLIP|")) {
                        resp = resp.substring(7, resp.length() - 1);
                        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);

                        clipboard.setText(resp.toString());
                    } else if (resp.length() > 6
                            && resp.substring(0, 6).equals("~SHOW|")) {
                        resp = resp.substring(6, resp.length() - 1);
                        showTextOnScreen(resp);
                    } else if (resp.length() > 15
                            && (resp.substring(0, 15).equals("~SERVERSTARTED|") || resp
                            .substring(0, 15).equals("~SERVERSTOPPED|"))) {
                        if (resp.substring(0, 15).equals("~SERVERSTARTED|"))
                            serverOn = true;
                        else
                            serverOn = false;
                        Intent intent = new Intent("com.delion.browser.ACTIVITY_UPDATE");
                        sendBroadcast(intent);
                    }
                    else if (preferences.getBoolean("notifyclient", true)) {
                        if (resp.length() > 10
                                && resp.substring(0, 11).equals("~CONNECTED|")) {
                            resp = resp.substring(11, resp.length() - 1);
                            showClientConnected(resp);
                        } else if (resp.length() > 13
                                && resp.substring(0, 14).equals(
                                "~DISCONNECTED|")) {
                            showClientDisconnected();
                        }
                    } else {
                        log("Received: " + resp);
                    }
                }
            } catch (IOException e) {
                log("ERROR em SOCKETLISTEN " + e.getMessage());
            }
        }
    }

    public void showClientConnected(String c) {
        String ns = Context.NOTIFICATION_SERVICE;
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(ns);

      //  int icon = R.drawable.icon;
        CharSequence tickerText = c + " connected to VNC server";
        long when = System.currentTimeMillis();

        Context context = getApplicationContext();
        CharSequence contentTitle = "RemoteAssistance Server";
        CharSequence contentText = "Client Connected from " + c;
        Intent notificationIntent = new Intent();
        PendingIntent contentIntent = PendingIntent.getActivity(
                getApplicationContext(), 0, notificationIntent, 0);

        //* lower than API 16 or 11,use this method.wqm ,20170121
      /*  Notification notification = new Notification(icon, tickerText, when);
        notification.setLatestEventInfo(context, contentTitle, contentText,
                contentIntent);*/


		/*Notification notification = new Notification.Builder(context)
		         .setAutoCancel(true)
		         .setTicker(tickerText)
		         .setContentTitle(contentTitle)
		         .setContentText(contentText)
		         .setContentIntent(contentIntent)
		         .setSmallIcon(icon)
		         .setWhen(when)
		         .build();

        mNotificationManager.notify(NewTabPageView.APP_ID, notification);*/

        // lets see if we should keep screen on
        if (preferences.getBoolean("screenturnoff", false)) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            wakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "VNC");
            wakeLock.acquire();
        }
    }

    void showClientDisconnected() {
        String ns = Context.NOTIFICATION_SERVICE;
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(ns);
        mNotificationManager.cancel(NewTabPageView.APP_ID);

        if (wakeLock != null && wakeLock.isHeld())
            wakeLock.release();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //showTextOnScreen("Droid VNC server service killed...");
    }

    static void writeCommand(OutputStream os, String command) throws Exception {
        os.write((command + "\n").getBytes("ASCII"));
    }

    public void showTextOnScreen(final String t) {
        handler.post(new Runnable() {

            public void run() {
                // TODO Auto-generated method stub
                Toast.makeText(getApplicationContext(), t, Toast.LENGTH_LONG)
                        .show();
            }
        });
    }

    public void log(String s) {
        Log.v(NewTabPageView.VNC_LOG, s);
    }



    // We return the binder class upon a call of bindService
    @Override
    public IBinder onBind(Intent arg0) {
        return mBinder;
    }

    public class MyBinder extends Binder {
        public ServerManager getService() {
            return ServerManager.this;
        }
    }
    public interface ServerRunningTestCallBack {
        void testResult(Boolean state);
    }
}