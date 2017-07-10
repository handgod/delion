package org.chromium.chrome.browser.vnc.reg;

public class RegistRequest {
    private String imei;

    private String uniqueId;

    private String otherSessionId;

    private String sign;

    public RegistRequest() {
    }

    public RegistRequest(String otherSessionId) {
        this.otherSessionId = otherSessionId;
    }

    public String getImei() {
        return imei;
    }

    public void setImei(String imei) {
        this.imei = imei;
    }

    public String getUniqueId() {
        return uniqueId;
    }

    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
    }

    public String getSign() {
        return sign;
    }

    public void setSign(String sign) {
        this.sign = sign;
    }

    public String getOtherSessionId() {
        return otherSessionId;
    }

    public void setOtherSessionId(String otherSessionId) {
        this.otherSessionId = otherSessionId;
    }
}
