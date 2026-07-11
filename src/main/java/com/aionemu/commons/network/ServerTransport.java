package com.aionemu.commons.network;

/**
 * 服务端/客户端传输生命周期接口。
 * Server/client transport lifecycle interface.
 */
public interface ServerTransport {

    /**
     * 建立连接或绑定监听。
     * Establish connection or bind listener.
     */
    void connect();

    /**
     * 关闭传输并释放资源。
     * Shutdown transport and release resources.
     */
    void shutdown();

    /**
     * 获取当前活跃连接数。
     * Get number of active connections.
     *
     * @return 活跃连接数 / Active connection count
     */
    int getActiveConnections();
}
