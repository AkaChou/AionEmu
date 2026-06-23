package com.aionemu.commons.network;

import java.io.IOException;
import java.nio.channels.SocketChannel;

/**
 * 连接工厂接口
 * Connection Factory Interface
 *
 * 该接口定义了创建网络连接实例的工厂方法
 * This interface defines the factory method for creating network connection instances
 */
public interface ConnectionFactory {

    /**
     * 创建新的网络连接实例
     * Create a new network connection instance
     *
     * @param socketChannel Socket通道 / The socket channel
     * @param dispatcher 调度器 / The dispatcher
     * @return 新创建的连接实例 / The newly created connection instance
     * @throws IOException 如果创建连接时发生IO错误 / If an IO error occurs while creating the connection
     */
    AConnection create(SocketChannel socketChannel, Dispatcher dispatcher) throws IOException;
}
