package org.chromium.chrome.browser.vnc.vnc;

import android.graphics.Bitmap;
import android.util.DisplayMetrics;
import android.util.Log;

import org.chromium.chrome.browser.ChromeApplication;
import org.chromium.chrome.browser.util.FormatTransfer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;

public class ScreenCap {
    private static ByteBuffer byteBuffer;
    private static boolean bInit = false;
    private static int width, height;
    private static Method screenshot;
    private static int length;
    private static Bitmap mScreenBitmap = null;
    public static byte[] capture() {
        if (!bInit) {
            try {
                DisplayMetrics displayMetrics = ChromeApplication.getInstance().getResources().getDisplayMetrics();
                width = displayMetrics.widthPixels;
                height = displayMetrics.heightPixels;
                length = width * height;
                byteBuffer = ByteBuffer.allocate(length * 2);
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                    Class surfaceControl = null;
                    surfaceControl = Class.forName("android.view.SurfaceControl");
                    screenshot = surfaceControl.getMethod("screenshot", new Class[]{int.class, int.class});
                } else {
                    Class surfaceClass = Class.forName("android.view.Surface");
                    screenshot = surfaceClass.getMethod("screenshot", new Class[]{int.class, int.class});
                }
                if (screenshot != null) {
                    screenshot.setAccessible(true);
                }
                bInit = true;
            } catch (Exception e) {
                e.printStackTrace();
                Log.d("VNC", "Init capture failed:" + e.getMessage());
                return null;
            }
        }

        try {
            mScreenBitmap = (Bitmap) screenshot.invoke(null, width, height);
        } catch (Exception e) {
            e.printStackTrace();
            Log.d("VNC", "capture screenshot invoke error:" + e.getMessage());
            return null;
        }
        //long pixelStart = SystemClock.uptimeMillis();
        Bitmap mScreenBitmap565 = mScreenBitmap.copy(Bitmap.Config.RGB_565, false);
        //saveMyBitmap(String.valueOf(i), mScreenBitmap565);
        byteBuffer.clear();
        mScreenBitmap565.copyPixelsToBuffer(byteBuffer);
        if (mScreenBitmap != null) {
            mScreenBitmap.recycle();
        }
        if (mScreenBitmap565 != null) {
            mScreenBitmap565.recycle();
        }
        //long pixelEnd = SystemClock.uptimeMillis();
        //Log.d("VNC", "ScreenCap pixel used:" + (pixelEnd - pixelStart) + "ms");
        return byteBuffer.array();
    }

    /*
     * screenFormat
     * uint16_t width;
     * uint16_t height;
     * uint8_t bitsPerPixel;
     * uint16_t redMax;
     * uint16_t greenMax;
     * uint16_t blueMax;
     * uint16_t alphaMax;
     * uint8_t redShift;
     * uint8_t greenShift;
     * uint8_t blueShift;
     * uint8_t alphaShift;
     * uint32_t size;
     * uint32_t pad;
    */
    public static byte[] format() {
        byte[] formatBuf = new byte[25];
        DisplayMetrics displayMetrics = ChromeApplication.getInstance().getApplicationContext().getResources().getDisplayMetrics();
        short width = (short) displayMetrics.widthPixels;
        short height = (short) displayMetrics.heightPixels;
        /*
        byte bitsPerPixel = 32;
        short redMax = 0x8;
        short greenMax = 0x8;
        short blueMax = 0x8;
        short alphaMax = 0x8;
        byte redShift = 8;
        byte greenShift = 16;
        byte blueShift = 24;
        byte alphaShift = 0;*/

        byte bitsPerPixel = 16;
        short redMax = 0x5;
        short greenMax = 0x6;
        short blueMax = 0x5;
        short alphaMax = 0x0;
        byte redShift = 11;
        byte greenShift = 5;
        byte blueShift = 0;
        byte alphaShift = 0;


        int size = width * height * bitsPerPixel / 8;
        int pad = 0;

        int index = 0;
        System.arraycopy(FormatTransfer.toHH(width), 0, formatBuf, index, 2);
        index += 2;
        System.arraycopy(FormatTransfer.toHH(height), 0, formatBuf, index, 2);
        index += 2;
        System.arraycopy(FormatTransfer.toHH(bitsPerPixel), 0, formatBuf, index, 1);
        index += 1;
        System.arraycopy(FormatTransfer.toHH(redMax), 0, formatBuf, index, 2);
        index += 2;
        System.arraycopy(FormatTransfer.toHH(greenMax), 0, formatBuf, index, 2);
        index += 2;
        System.arraycopy(FormatTransfer.toHH(blueMax), 0, formatBuf, index, 2);
        index += 2;
        System.arraycopy(FormatTransfer.toHH(alphaMax), 0, formatBuf, index, 2);
        index += 2;
        System.arraycopy(FormatTransfer.toHH(redShift), 0, formatBuf, index, 1);
        index += 1;
        System.arraycopy(FormatTransfer.toHH(greenShift), 0, formatBuf, index, 1);
        index += 1;
        System.arraycopy(FormatTransfer.toHH(blueShift), 0, formatBuf, index, 1);
        index += 1;
        System.arraycopy(FormatTransfer.toHH(alphaShift), 0, formatBuf, index, 1);
        index += 1;
        System.arraycopy(FormatTransfer.toHH(size), 0, formatBuf, index, 4);
        index += 4;
        System.arraycopy(FormatTransfer.toHH(pad), 0, formatBuf, index, 4);
        return formatBuf;
    }

    /**
     * Test
     *
     * @param bitName
     * @param mBitmap
     */
    public static void saveMyBitmap(String bitName, Bitmap mBitmap) {
        File f = new File("/sdcard/bitmap/" + bitName + ".png");
        try {
            f.createNewFile();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            Log.d("VNC", "在保存图片时出错：" + e.toString());
        }
        FileOutputStream fOut = null;
        try {
            fOut = new FileOutputStream(f);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        mBitmap.compress(Bitmap.CompressFormat.PNG, 100, fOut);
        try {
            fOut.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            fOut.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
