package org.chromium.chrome.browser.util;

/**
 * Created by liangjun on 2017/3/17.
 */

public class FormatTransfer {
    public static byte[] toLH(int n) {
        byte[] b = new byte[4];
        b[0] = (byte) (n & 0xff);
        b[1] = (byte) (n >> 8 & 0xff);
        b[2] = (byte) (n >> 16 & 0xff);
        b[3] = (byte) (n >> 24 & 0xff);
        return b;
    }


    public static byte[] toHH(int n) {
        byte[] b = new byte[4];
        b[3] = (byte) (n & 0xff);
        b[2] = (byte) (n >> 8 & 0xff);
        b[1] = (byte) (n >> 16 & 0xff);
        b[0] = (byte) (n >> 24 & 0xff);
        return b;
    }


    public static byte[] toLH(short n) {
        byte[] b = new byte[2];
        b[0] = (byte) (n & 0xff);
        b[1] = (byte) (n >> 8 & 0xff);
        return b;
    }


    public static byte[] toHH(short n) {
        byte[] b = new byte[2];
        b[1] = (byte) (n & 0xff);
        b[0] = (byte) (n >> 8 & 0xff);
        return b;
    }
    public static byte[] toLH(byte n) {
        byte[] b = new byte[1];
        b[0] = (byte) (n & 0xff);
        return b;
    }


    public static byte[] toHH(byte n) {
        byte[] b = new byte[1];
        b[0] = (byte) (n & 0xff);
        return b;
    }

    public static byte[] toLH(float f) {
        return toLH(Float.floatToRawIntBits(f));
    }

    public static byte[] toHH(float f) {
        return toHH(Float.floatToRawIntBits(f));
    }
}
