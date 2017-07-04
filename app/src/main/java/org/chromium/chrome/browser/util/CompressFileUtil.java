package org.chromium.chrome.browser.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Created by liangjun on 2017/3/14.
 */

public class CompressFileUtil {
    public static boolean compressFile(String resourcePath, String targetPath) throws Exception {
        File resourceFile = new File(resourcePath);     //源文件
        File targetFile = new File(targetPath);           //目的
        if(!resourceFile.exists()){
            return false;
        }
        //如果目的路径不存在，则新建
        if (!targetFile.exists()) {
            targetFile.createNewFile();
        }
        //文件输入流
        InputStream inputStream = new BufferedInputStream(new FileInputStream(resourceFile));
        //文件输出流
        GZIPOutputStream out = new GZIPOutputStream(new BufferedOutputStream(new FileOutputStream(targetFile)));
        //进行写操作
        int len = 0;
        byte[] buffer = new byte[1024];
        while ((len = inputStream.read(buffer)) > 0) {
            out.write(buffer, 0, len);
        }
        //关闭输入流
        inputStream.close();
        out.close();
        return true;
    }
    public static boolean uncompressFile(String resourcePath, String targetPath) throws Exception {
        File resourceFile = new File(resourcePath);     //源文件
        File targetFile = new File(targetPath);           //目的
        if(!resourceFile.exists()){
            return false;
        }
        //如果目的路径不存在，则新建
        if (!targetFile.exists()) {
            targetFile.createNewFile();
        }
        //文件输入流
        GZIPInputStream inputStream = new GZIPInputStream(new BufferedInputStream(new FileInputStream(resourceFile)));
        //文件输出流
        OutputStream out = new BufferedOutputStream(new FileOutputStream(targetFile));
        //进行写操作
        int len = 0;
        byte[] buffer = new byte[1024];
        while ((len = inputStream.read(buffer)) > 0) {
            out.write(buffer, 0, len);
        }
        //关闭输入流
        inputStream.close();
        out.close();
        return true;
    }

}
