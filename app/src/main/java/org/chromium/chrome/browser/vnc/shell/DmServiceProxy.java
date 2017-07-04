package org.chromium.chrome.browser.vnc.shell;

import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;

import static org.chromium.chrome.browser.vnc.shell.IDmService.RUN_SHELL_TRANSACTION;
import static org.chromium.chrome.browser.vnc.shell.IDmService.descriptor;

/** @hide */
public class DmServiceProxy implements IDmService {

    /** @hide */
    public static DmServiceProxy asInterface(android.os.IBinder obj){
        if (obj == null) {
            return null;
        }
        DmServiceProxy in =
                (DmServiceProxy)obj.queryLocalInterface(descriptor);
        if (in != null) {
            return in;
        }

        return new DmServiceProxy(obj);
    }

    /** @hide */
    public DmServiceProxy(IBinder remote) {
        mRemote = remote;
    }

    /** @hide */
    public IBinder asBinder() {
        return mRemote;
    }

    /** @hide */
    public int runShell(String shell) throws RemoteException {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        data.writeInterfaceToken(descriptor);
        data.writeString(shell);
        mRemote.transact(RUN_SHELL_TRANSACTION, data, reply, 0);
        int ret = reply.readInt();
        reply.recycle();
        data.recycle();
        return ret;
    }

    /** @hide */
    /** return no more than 2048 bytes **/
    public String runShellWithResult(String shell) throws RemoteException {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        data.writeInterfaceToken(IDmService.descriptor);
        data.writeString(shell);
        mRemote.transact(RUN_SHELL_WITH_RESULT_TRANSACTION, data, reply, 0);
        String ret = reply.readString();
        reply.recycle();
        data.recycle();
        return ret;
    }

    /** @hide */
    public int runShellFile(String path) throws RemoteException {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        data.writeInterfaceToken(IDmService.descriptor);
        data.writeString(path);
        mRemote.transact(RUN_SHELL_FILE_TRANSACTION, data, reply, 0);
        int ret = reply.readInt();
        reply.recycle();
        data.recycle();
        return ret;
    }

    private IBinder mRemote;
}

