package org.chromium.chrome.browser.vnc.vnc;

import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import org.chromium.chrome.browser.ChromeApplication;
import org.chromium.chrome.browser.vnc.executor.ThreadExecutor;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import static android.content.Context.TELEPHONY_SERVICE;

public class RemoteClientProxy {
    private String opcode;
    private static final String TAG = "RemoteConnectService";
    Executor serverExecutor = Executors.newSingleThreadExecutor();
    public static Socket clientSocket;
    private ServerSocket serverSocket;
    private static final int PROXY_PORT = 1234;
    private static int DATA_TYPE_HEARTBEAT = 0;
    private static int DATA_TYPE_VNC = 1;

    public RemoteClientProxy() {
        try {
/*            serverSocket = new ServerSocket(PROXY_PORT);
            serverSocket.setReuseAddress(true);*/
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, "ServerSocket error");
        }
    }

    private final String SERVER_HOST = "127.0.0.1";
    private final int SERVER_PORT = 5901;
    private Socket serverProxySocket;
    private byte[] recvBuf = new byte[10240];
    private byte[] sendBuf = new byte[10240];

    private AtomicBoolean bReadClientToServer = new AtomicBoolean(true);
    private AtomicBoolean bReadServerToClient = new AtomicBoolean(true);
    private ExecutorService sendAndRecvThreadPool = Executors.newFixedThreadPool(2);
    private ExecutorService stunExecutor = Executors.newSingleThreadExecutor();
    private DataInputStream clientInputStream;
    private DataOutputStream clientOutputStream;

    private DataInputStream serverInputStream;
    private DataOutputStream serverOutputStream;
    private AtomicBoolean bConnectState = new AtomicBoolean(false);

    public void getOpcode() {
        stunExecutor.execute(new Runnable() {
            @Override
            public void run() {
                //openTrickStunSocket();
                TelephonyManager TelephonyMgr = (TelephonyManager) ChromeApplication.getInstance().getSystemService(TELEPHONY_SERVICE);
                String strImei = TelephonyMgr.getDeviceId();
                if (TextUtils.isEmpty(strImei)) {
                    strImei = "random_" + String.valueOf(new Random().nextInt());
                }
               /* if(NetState.getInstance(ChromeApplication.getInstance()).isOpenNetwork()) {
                    NatClientBootstrap.main(new String[]{strImei, strImei, ""});
                }else {
                    NatStateDispatcher.getInstance().notifyOpcode(false, "network unavailable");
                }*/
            }
        });
    }

    public void startConnect() throws Exception {
        Log.d(TAG, "RemoteClientProxy startConnect");
        ThreadExecutor.getInstance().submit(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1000);
                    clientInputStream = getInputStream();
                    clientOutputStream = getOutputStream();
                    serverProxySocket = new Socket(SERVER_HOST, SERVER_PORT);
                    serverInputStream = new DataInputStream(serverProxySocket.getInputStream());
                    serverOutputStream = new DataOutputStream(serverProxySocket.getOutputStream());
                    bReadClientToServer.set(Boolean.TRUE);
                    bReadServerToClient.set(Boolean.TRUE);
                    sendAndRecvThreadPool.execute(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                //Log.d(TAG, "read start");
                                while (bReadClientToServer.get()) {
                                    int type = clientInputStream.readInt();
                                    int length = clientInputStream.readInt();
                                    //Log.d(TAG, "clientInputStream.read:" + String.valueOf(length) + ", type:" + type);

                                    if (type == DATA_TYPE_HEARTBEAT) {//心跳
                                        int id = clientInputStream.readInt();
                                        synchronized (clientOutputStream) {//回心跳
                                            clientOutputStream.writeInt(DATA_TYPE_HEARTBEAT);
                                            clientOutputStream.writeInt(0);//心跳数据长度为0
                                            clientOutputStream.writeInt(id);//心跳id
                                            //Log.d(TAG, "rsp heartbeat:" + id);
                                        }
                                    } else {
                                        int recvNum = 0;
                                        while (recvNum < length) {
                                            int num = clientInputStream.read(recvBuf, recvNum, length - recvNum);
                                            if (num >= 0) {
                                                recvNum += num;
                                            } else {
                                          //      LOGGER.debug("clientInputStream.read end");
                                                disconnect();
                                                return;
                                            }
                                        }
                                        serverOutputStream.write(recvBuf, 0, length);
                                    }

                                }

                            } catch (IOException e) {
                                e.printStackTrace();
                                Log.d(TAG, "ReadClientToServer exception:" + e.toString());
                                disconnect();
                            }
                        }
                    });

                    sendAndRecvThreadPool.execute(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                int count = 0;
                                //Log.d(TAG, "write start");
                                while (bReadServerToClient.get()) {
                                    count = serverInputStream.read(sendBuf);
                                    //Log.d(TAG, "serverInputStream.read:" + String.valueOf(count));
                                    if (count > 0) {
                                        synchronized (clientOutputStream) {
                                            //Log.d(TAG, "clientOutputStream.write:" + count + ", type:1");
                                            clientOutputStream.writeInt(DATA_TYPE_VNC);
                                            clientOutputStream.writeInt(count);
                                            clientOutputStream.write(sendBuf, 0, count);
                                            //Log.d(TAG, "clientOutputStream.write ok");
                                        }
                                    } else {
                                        disconnect();
                                    }
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                                Log.d(TAG, "ReadServerToClient exception:" + e.toString());
                            }
                        }
                    });
                } catch (Exception e) {
                    Log.d(TAG, "startConnect prepare stream exception:" + e.toString());
                }
                bConnectState.set(Boolean.TRUE);
                Log.d(TAG, "startConnect success");
            }
        });
    }

    public void disconnect() {
        Log.d(TAG, "call disconnect");
        if (bConnectState.get()) {
            bReadClientToServer.set(Boolean.FALSE);
            bReadServerToClient.set(Boolean.FALSE);
            try {
                clientOutputStream.close();
                serverProxySocket.getInputStream().close();
                clientInputStream.close();
                serverProxySocket.getOutputStream().close();
            } catch (Exception e) {
                Log.d(TAG, "disconnect:" + e.toString());
            }
        }
        bConnectState.set(Boolean.FALSE);
    }

    private DataInputStream getInputStream() throws Exception {
        return new DataInputStream(clientSocket.getInputStream());
    }

    private DataOutputStream getOutputStream() throws Exception {
        return new DataOutputStream(clientSocket.getOutputStream());
    }

/*    private void openTrickStunSocket() {
        VNCCachedThreadPool.getInstance().submit(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(2000);
                    EventBus.getDefault().post(new OpcodeEvent("110112"));
                    Log.d(TAG, "openTrickStunSocket waiting...");
                    ServerSocket sever = new ServerSocket(4325);
                    Socket client = sever.accept();
                    Log.d(TAG, "openTrickStunSocket client connected");
                    EventBus.getDefault().post(ConSchemeEvent.createConStunEvent("110119", client));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }*/
}
