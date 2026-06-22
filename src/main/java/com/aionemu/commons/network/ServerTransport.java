package com.aionemu.commons.network;

public interface ServerTransport {

    void connect();

    void shutdown();

    int getActiveConnections();
}
