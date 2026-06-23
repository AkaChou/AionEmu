package com.aionemu.commons.network;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

/**
 * 网络连接接收器
 * Network Connection Acceptor
 *
 * 该类负责处理新的网络连接请求，创建连接实例并注册到调度器
 * This class is responsible for handling new network connection requests,
 * creating connection instances and registering them with the dispatcher
 */
public class Acceptor {
    
    /**
     * 连接工厂，用于创建新的连接实例
     * Connection factory for creating new connection instances
     */
    private final ConnectionFactory factory;
    
    /**
     * NIO服务器实例的引用
     * Reference to the NIO server instance
     */
    private final NioServer nioServer;

    /**
     * 构造函数
     * Constructor
     *
     * @param factory 连接工厂实例 / Connection factory instance
     * @param nioServer NIO服务器实例 / NIO server instance
     */
    Acceptor(ConnectionFactory factory, NioServer nioServer) {
        this.factory = factory;
        this.nioServer = nioServer;
    }

    /**
     * 接受新的连接请求
     * Accept new connection request
     *
     * @param key 选择键 / Selection key representing the server socket channel
     * @throws IOException 如果在接受连接时发生IO错误 / If an IO error occurs while accepting the connection
     */
    public final void accept(SelectionKey key) throws IOException {
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel)key.channel();
        SocketChannel socketChannel = serverSocketChannel.accept();
        socketChannel.configureBlocking(false);
        Dispatcher dispatcher = this.nioServer.getReadWriteDispatcher();
        AConnection con = this.factory.create(socketChannel, dispatcher);
        if (con != null) {
            dispatcher.register(socketChannel, 1, con);
            con.initialized();
        }
    }
}
