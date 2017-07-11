package org.chromium.chrome.browser.vnc.reg;

public class RegistResponse {

    private String IMEI;
    private String RepeaterIpAddress;
    private String USERID;
    public RegistResponse() {
    }

    //{"regInfo":{"IMEI":"1234567890987665","RepeaterIpAddress":"118.89.48.252","uniqueId":"123456"}}
    public RegistResponse(String IMEI, String RepeaterIpAddress,String USERID) {
        this.IMEI = IMEI;
        this.RepeaterIpAddress = RepeaterIpAddress;
        this.USERID = USERID;

    }

    public String getIMEI() {
        return IMEI;
    }

    public void setIMEI(String IMEI) {
        this.IMEI = IMEI;
    }

    public String getUSERID() {
        return USERID;
    }

    public void setUSERID(String USERID) {
        this.USERID = USERID;
    }

    public String getRepeaterIpAddress() {
        return RepeaterIpAddress;
    }

    public void setRepeaterIpAddress(String RepeaterIpAddress) {
        this.RepeaterIpAddress = RepeaterIpAddress;
    }

}
