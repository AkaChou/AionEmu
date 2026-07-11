package com.aionemu.commons.network;

import java.io.IOException;

/**
 * Netty 连接工厂，根据传输层创建业务连接实例。
 * Netty connection factory that creates business connections from transport.
 */
public interface NettyConnectionFactory {

    /**
     * 创建连接实例。
     * Create a connection instance.
     *
     * Transport layer
     * @return 连接实例，拒绝时可为 null / Connection instance, or null when rejected
     * Creation failure
     */
    AConnection create(ConnectionTransport transport) throws IOException;
}
