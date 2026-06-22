package com.aionemu.boot.transport;

import java.net.InetSocketAddress;

public final class AionNettyEndpoint {

    private final String name;
    private final String host;
    private final int port;

    private AionNettyEndpoint(String name, String host, int port) {
        this.name = name;
        this.host = host;
        this.port = port;
    }

    public static AionNettyEndpoint of(String name, String host, int port) {
        return new AionNettyEndpoint(name, host, port);
    }

    public String getName() {
        return name;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public InetSocketAddress getAddress() {
        if ("*".equals(host)) {
            return new InetSocketAddress(port);
        }
        return new InetSocketAddress(host, port);
    }
}
