package com.aionemu.commons.network;


/**
 * Netty 连接工厂，根据传输层创建业务连接实例。
 * Netty connection factory that creates business connections from transport.
 */
public interface NettyConnectionFactory {

    /**
     * 创建连接实例。
     * Create a connection instance.
     * @param transport 传输层 / Transport layer
     * @return 连接实例，拒绝时可为 null / Connection instance, or null when rejected
     */
    AConnection create(ConnectionTransport transport);
}
