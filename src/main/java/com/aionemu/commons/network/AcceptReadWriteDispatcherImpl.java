package com.aionemu.commons.network;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * 接受、读写操作的调度器实现类
 * Implementation of the dispatcher that handles accepting, reading and writing operations
 *
 * 该类负责处理网络连接的接受、读取和写入操作，同时管理待关闭的连接
 * This class is responsible for handling network connection acceptance, reading and writing operations,
 * while also managing connections pending closure
 */
public class AcceptReadWriteDispatcherImpl extends Dispatcher {

    /**
     * 待关闭的连接列表
     * List of connections pending closure
     */
    private final List<AConnection> pendingClose = new ArrayList<AConnection>();

    /**
     * 构造函数
     * Constructor
     *
     * @param name 调度器名称 / The name of the dispatcher
     * @param dcPool 断开连接线程池 / Thread pool for handling disconnections
     * @throws IOException 如果初始化selector失败 / If selector initialization fails
     */
    public AcceptReadWriteDispatcherImpl(String name, Executor dcPool) throws IOException {
        super(name, dcPool);
    }

    /**
     * 调度方法，处理接受、读写操作的逻辑
     * Dispatch method that handles the logic for accepting, reading and writing operations
     *
     * @throws IOException 如果在处理IO操作时发生错误 / If an error occurs during IO operations
     */
    @Override
    void dispatch() throws IOException {
        int selected = this.selector.select();
        this.processPendingClose();
        if (selected != 0) {
            Iterator<SelectionKey> selectedKeys = this.selector.selectedKeys().iterator();

            while (selectedKeys.hasNext()) {
                SelectionKey key = selectedKeys.next();
                selectedKeys.remove();
                if (key.isValid()) {
                    switch (key.readyOps()) {
                        case SelectionKey.OP_READ:
                            this.read(key);
                            break;
                        case SelectionKey.OP_WRITE:
                            this.write(key);
                            break;
                        case SelectionKey.OP_READ | SelectionKey.OP_WRITE:
                            this.read(key);
                            if (key.isValid()) {
                                this.write(key);
                            }
                            break;
                        case SelectionKey.OP_ACCEPT:
                            this.accept(key);
                    }
                }
            }
        }
    }

    /**
     * 关闭连接，将连接添加到待关闭列表
     * Close connection by adding it to the pending close list
     *
     * @param con 要关闭的连接 / The connection to close
     */
    @Override
    void closeConnection(AConnection con) {
        synchronized (this.pendingClose) {
            this.pendingClose.add(con);
        }
    }

    /**
     * 处理待关闭的连接
     * Process connections pending closure
     */
    private void processPendingClose() {
        synchronized (this.pendingClose) {
            for (AConnection connection : this.pendingClose) {
                this.closeConnectionImpl(connection);
            }
            this.pendingClose.clear();
        }
    }
}
