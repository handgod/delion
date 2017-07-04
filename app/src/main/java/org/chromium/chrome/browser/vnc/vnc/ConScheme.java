package org.chromium.chrome.browser.vnc.vnc;

public enum ConScheme {
    STUN(1),REPEATER(2);

    private final int value;

    ConScheme(int value) {
        this.value = value;
    }
    public int getConScheme(){
        return value;
    }
}
