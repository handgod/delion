package org.chromium.chrome.browser.vnc.reg;

public class RegistResponse {
    private Long userId;

    private String sessionId;

    private byte[] pubKey;

    private AddressInfo natServer;

    private String repeater;

    public RegistResponse() {
    }

    public RegistResponse(Long userId, String sessionId, byte[] pubKey,
                          AddressInfo natServer, String repeater) {
        this.userId = userId;
        this.sessionId = sessionId;
        this.pubKey = pubKey;
        this.natServer = natServer;
        this.repeater = repeater;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public AddressInfo getNatServer() {
        return natServer;
    }

    public void setNatServer(AddressInfo natServer) {
        this.natServer = natServer;
    }

    public byte[] getPubKey() {
        return pubKey;
    }

    public void setPubKey(byte[] pubKey) {
        this.pubKey = pubKey;
    }

    public String getRepeater() {
        return repeater;
    }

    public void setRepeater(String repeater) {
        this.repeater = repeater;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
