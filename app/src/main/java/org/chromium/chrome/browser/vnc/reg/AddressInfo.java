package org.chromium.chrome.browser.vnc.reg;


import java.net.InetSocketAddress;

public class AddressInfo {
    private String ip;

    private int port;

    public AddressInfo() {
    }

    public AddressInfo(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String toString(){
        return this.ip + Constants.SIGN_COLON + this.port;
    }

    public boolean equals(AddressInfo info){
        return this.ip.equals(info.getIp()) && this.port == info.getPort();
    }

    public InetSocketAddress toInetAddress(){
        return new InetSocketAddress(ip, port);
    }
}
