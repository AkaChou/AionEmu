package com.aionemu.commons.network;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.util.Iterator;
import java.util.concurrent.Executor;

/**
 * 接受连接的调度器实现类
 * Implementation of the dispatcher that handles accepting new connections
 *
 * 该类负责监听和接受新的网络连接请求，是NIO服务器接受连接的核心组件
 * This class is responsible for listening and accepting new network connection requests,
 * serving as the core component for accepting connections in the NIO server
 */
public class AcceptDispatcherImpl extends Dispatcher {

    /**
     * 构造函数
     * Constructor
     *
     * @param name 调度器名称 / The name of the dispatcher
     * @throws IOException 如果初始化selector失败 / If selector initialization fails
     */
    public AcceptDispatcherImpl(String name) throws IOException {
        super(name, null);
    }

    /**
     * 调度方法，处理接受连接的逻辑
     * Dispatch method that handles the logic for accepting connections
     *
     * @throws IOException 如果在处理连接时发生IO错误 / If an IO error occurs while handling connections
     */
    @Override
    void dispatch() throws IOException {
        if (this.selector.select() != 0) {
            Iterator<SelectionKey> selectedKeys = this.selector.selectedKeys().iterator();

            while (selectedKeys.hasNext()) {
                SelectionKey key = selectedKeys.next();
                selectedKeys.remove();
                if (key.isValid()) {
                    this.accept(key);
                }
            }
        }
    }

    /**
     * 关闭连接的方法，在此实现中不支持
     * Method for closing connections, not supported in this implementation
     *
     * @param con 要关闭的连接 / The connection to close
     * @throws UnsupportedOperationException 该方法不应被调用 / This method should never be called
     */
    @Override
    void closeConnection(AConnection con) {
        throw new UnsupportedOperationException("This method should never be called!");
    }
}
