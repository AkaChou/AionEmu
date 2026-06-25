package com.aionemu.commons.network;

import com.aionemu.commons.services.ServiceContext;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * 抽象网络连接基类
 * Abstract base class for network connections
 *
 * 该类提供了网络连接的基本功能，包括读写缓冲区管理、连接状态控制等
 * This class provides basic functionality for network connections,
 * including read/write buffer management and connection state control
 */
public abstract class AConnection {
    
    private final ConnectionTransport transport;
    private final String serviceContext;
    
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

    protected AConnection(ConnectionTransport transport, int rbSize, int wbSize) {
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
     * 启用写操作兴趣
     * Enable write operation interest
     */
    protected final void enableWriteInterest() {
        this.transport.enableWriteInterest();
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
                this.transport.close(forced);
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
            return this.transport.onlyClose();
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
