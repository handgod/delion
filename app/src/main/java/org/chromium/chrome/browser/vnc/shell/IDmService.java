package org.chromium.chrome.browser.vnc.shell;

import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteException;

public interface IDmService extends IInterface
{
    /** @hide */
    public int runShell(String shell) throws RemoteException;
    public String runShellWithResult(String shell) throws RemoteException;
    public int runShellFile(String path) throws RemoteException;

    static final String descriptor = "IDmService";//"android.os.IDmService";

    int RUN_SHELL_TRANSACTION = IBinder.FIRST_CALL_TRANSACTION;

    /** should NEVER!!! remount ro disk with RUN_SHELL_FILE_TRANSACTION,
     please use  RUN_SHELL_TRANSACTION instead  */
    int RUN_SHELL_FILE_TRANSACTION = IBinder.FIRST_CALL_TRANSACTION + 1;
    int RUN_SHELL_WITH_RESULT_TRANSACTION = IBinder.FIRST_CALL_TRANSACTION + 2;
}
