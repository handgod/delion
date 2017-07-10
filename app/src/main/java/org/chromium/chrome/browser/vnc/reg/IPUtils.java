package org.chromium.chrome.browser.vnc.reg;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public class IPUtils {

    public final static String LOCAL_IP;

    public final static String INTERNET_IP;

    static {
        LOCAL_IP = getLocalIp();
        INTERNET_IP = getInternetIp();
    }

    public static void main(String[] args) {
        System.out.println(LOCAL_IP);
        System.out.println(INTERNET_IP);
    }

    public static String getLocalIp() {
        Enumeration<NetworkInterface> netInterfaces = null;
        try {
            netInterfaces = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {

        }
        while (netInterfaces != null && netInterfaces.hasMoreElements()) {
            NetworkInterface ni = netInterfaces.nextElement();
            Enumeration<InetAddress> address = ni.getInetAddresses();
            while (address.hasMoreElements()) {
                InetAddress ip = address.nextElement();
                if (ip.isSiteLocalAddress() && !ip.isLoopbackAddress() && ip.getHostAddress().indexOf(":") == -1) {// 内网IP
                    return ip.getHostAddress();
                }
            }
        }
        return null;
    }

    public static String getInternetIp() {
        String localip = null;// 本地IP，如果没有配置外网IP则返回它

        Enumeration<NetworkInterface> netInterfaces = null;
        try {
            netInterfaces = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
        }
        while (netInterfaces != null && netInterfaces.hasMoreElements()) {
            NetworkInterface ni = netInterfaces.nextElement();
            Enumeration<InetAddress> address = ni.getInetAddresses();
            while (address.hasMoreElements()) {
                InetAddress ip = address.nextElement();
                if (!ip.isSiteLocalAddress() && !ip.isLoopbackAddress() && ip.getHostAddress().indexOf(":") == -1) {// 外网IP
                    return ip.getHostAddress();
                } else if (localip == null && ip.isSiteLocalAddress() && !ip.isLoopbackAddress()
                        && ip.getHostAddress().indexOf(":") == -1) {// 内网IP
                    localip = ip.getHostAddress();
                }
            }
        }
        return localip;
    }
}
