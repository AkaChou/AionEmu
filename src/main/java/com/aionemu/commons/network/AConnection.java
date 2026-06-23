package com.aionemu.commons.network;

import com.aionemu.commons.services.ServiceContext;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

/**
 * 抽象网络连接基类
 * Abstract base class for network connections
 *
 * 该类提供了网络连接的基本功能，包括读写缓冲区管理、连接状态控制等
 * This class provides basic functionality for network connections,
 * including read/write buffer management and connection state control
 */
public abstract class AConnection {
    
    /**
     * Socket通道
     * The socket channel for this connection
     */
    private final SocketChannel socketChannel;
    
    /**
     * 调度器实例
     * Dispatcher instance
     */
    private final Dispatcher dispatcher;

    private final ConnectionTransport transport;
    private final String serviceContext;
    
    /**
     * 选择键
     * Selection key for this connection
     */
    private SelectionKey key;
    
    /**
     * 连接状态标志
     * Connection state flags
     */
    protected boolean pendingClose;
    protected boolean isForcedClosing;
    protected boolean closed;
    
    /**
     * 同步锁对象
     * Synchronization guard object
     */
    protected final Object guard = new Object();
    
    /**
     * 读写缓冲区
     * Read and write buffers
     */
    public final ByteBuffer writeBuffer;
    public final ByteBuffer readBuffer;
    
    /**
     * 连接IP地址
     * Connection IP address
     */
    private final String ip;
    
    /**
     * 连接锁定状态
     * Connection lock status
     */
    private boolean locked = false;

    /**
     * 构造函数
     * Constructor
     *
     * @param sc Socket通道 / Socket channel
     * @param d 调度器 / Dispatcher
     * @param rbSize 读缓冲区大小 / Read buffer size
     * @param wbSize 写缓冲区大小 / Write buffer size
     * @throws IOException 如果初始化失败 / If initialization fails
     */
    public AConnection(SocketChannel sc, Dispatcher d, int rbSize, int wbSize) throws IOException {
        this.socketChannel = sc;
        this.dispatcher = d;
        this.transport = null;
        this.serviceContext = ServiceContext.current();
        this.writeBuffer = ByteBuffer.allocate(wbSize);
        this.writeBuffer.flip();
        this.writeBuffer.order(ByteOrder.LITTLE_ENDIAN);
        this.readBuffer = ByteBuffer.allocate(rbSize);
        this.readBuffer.order(ByteOrder.LITTLE_ENDIAN);
        this.ip = this.socketChannel.socket().getInetAddress().getHostAddress();
    }

    protected AConnection(ConnectionTransport transport, int rbSize, int wbSize) {
        this.socketChannel = null;
        this.dispatcher = null;
        this.transport = transport;
        this.serviceContext = ServiceContext.current();
        this.writeBuffer = ByteBuffer.allocate(wbSize);
        this.writeBuffer.flip();
        this.writeBuffer.order(ByteOrder.LITTLE_ENDIAN);
        this.readBuffer = ByteBuffer.allocate(rbSize);
        this.readBuffer.order(ByteOrder.LITTLE_ENDIAN);
        this.ip = transport.getIP();
    }

    /**
     * 设置选择键
     * Set the selection key
     */
    final void setKey(SelectionKey key) {
        this.key = key;
    }

    /**
     * 启用写操作兴趣
     * Enable write operation interest
     */
    protected final void enableWriteInterest() {
        if (this.transport != null) {
            this.transport.enableWriteInterest();
            return;
        }
        if (this.key.isValid()) {
            this.key.interestOps(this.key.interestOps() | SelectionKey.OP_WRITE);
            this.key.selector().wakeup();
        }
    }

    /**
     * 获取调度器
     * Get the dispatcher
     */
    final Dispatcher getDispatcher() {
        return this.dispatcher;
    }

    /**
     * 获取Socket通道
     * Get the socket channel
     */
    public SocketChannel getSocketChannel() {
        return this.socketChannel;
    }

    /**
     * 关闭连接
     * Close the connection
     *
     * @param forced 是否强制关闭 / Whether to force close
     */
    public final void close(boolean forced) {
        synchronized(this.guard) {
            if (!this.isWriteDisabled()) {
                this.isForcedClosing = forced;
                if (this.transport != null) {
                    this.transport.close(forced);
                    return;
                }
                this.getDispatcher().closeConnection(this);
            }
        }
    }

    /**
     * 仅关闭连接
     * Only close the connection
     */
    final boolean onlyClose() {
        synchronized(this.guard) {
            if (this.closed) {
                return false;
            }
            if (this.transport != null) {
                return this.transport.onlyClose();
            }
            try {
                if (this.socketChannel.isOpen()) {
                    this.socketChannel.close();
                    this.key.attach(null);
                    this.key.cancel();
                }
                this.closed = true;
            } catch (IOException e) {
                // Ignore exception during close
            }
            return true;
        }
    }

    /**
     * 检查是否待关闭
     * Check if pending close
     */
    final boolean isPendingClose() {
        return this.pendingClose && !this.closed;
    }

    /**
     * 检查写操作是否禁用
     * Check if write is disabled
     */
    protected final boolean isWriteDisabled() {
        return this.pendingClose || this.closed;
    }

    /**
     * 获取IP地址
     * Get IP address
     */
    public final String getIP() {
        return this.ip;
    }

    public final String getServiceContext() {
        return this.serviceContext;
    }

    /**
     * 尝试锁定连接
     * Try to lock the connection
     */
    boolean tryLockConnection() {
        return !this.locked && (this.locked = true);
    }

    /**
     * 解锁连接
     * Unlock the connection
     */
    void unlockConnection() {
        this.locked = false;
    }

    /**
     * 处理接收到的数据
     * Process received data
     */
    protected abstract boolean processData(ByteBuffer buf);

    /**
     * 写出数据
     * Write data
     */
    protected abstract boolean writeData(ByteBuffer buf);

    /**
     * 初始化连接
     * Initialize connection
     */
    protected abstract void initialized();

    /**
     * 获取断开连接延迟
     * Get disconnection delay
     */
    protected abstract long getDisconnectionDelay();

    /**
     * 断开连接时的处理
     * Handle disconnection
     */
    protected abstract void onDisconnect();

    /**
     * 服务器关闭时的处理
     * Handle server shutdown
     */
    protected abstract void onServerClose();
}
