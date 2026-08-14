package com.aionemu.commons.network;

import com.aionemu.commons.services.ServiceContext;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * 抽象网络连接基类，管理读写缓冲与连接状态。
 * Abstract base class for network connections, managing buffers and connection state.
 */
public abstract class AConnection {

    private final ConnectionTransport transport;
    private final String serviceContext;

    /**
     * 连接状态标志。
     * Connection state flags.
     */
    protected boolean pendingClose;
    protected boolean isForcedClosing;
    protected boolean closed;

    /**
     * 同步锁对象。
     * Synchronization guard object.
     */
    protected final Object guard = new Object();

    /**
     * 读写缓冲区。
     * Read and write buffers.
     */
    public final ByteBuffer writeBuffer;
    public final ByteBuffer readBuffer;

    /**
     * 连接 IP 地址。
     * Connection IP address.
     */
    private final String ip;

    /**
     * 连接锁定状态（包处理互斥）。
     * Connection lock status for packet processing mutual exclusion.
     */
    private boolean locked = false;

    /**
     * 构造连接并初始化读写缓冲。
     * Construct connection and initialize read/write buffers.
     *
     * @param transport 传输层实现 / Transport implementation
     * @param rbSize 读缓冲大小 / Read buffer size
     * @param wbSize 写缓冲大小 / Write buffer size
     */
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
     * 启用写操作兴趣。
     * Enable write operation interest.
     */
    protected final void enableWriteInterest() {
        this.transport.enableWriteInterest();
    }

    /**
     * 关闭连接。
     * Close the connection.
     *
     * @param forced 是否强制关闭 / Whether to force close
     */
    public final void close(boolean forced) {
        synchronized (this.guard) {
            if (!this.isWriteDisabled()) {
                this.isForcedClosing = forced;
                this.transport.close(forced);
            }
        }
    }

    /**
     * 仅关闭底层连接（不重复关闭）。
     * Only close the underlying connection if not already closed.
     *
     * @return 是否实际执行了关闭 / Whether close was performed
     */
    final boolean onlyClose() {
        synchronized (this.guard) {
            if (this.closed) {
                return false;
            }
            return this.transport.onlyClose();
        }
    }

    /**
     * 检查是否待关闭且尚未关闭。
     * Check if pending close and not yet closed.
     *
     * @return 是否待关闭 / Whether pending close
     */
    final boolean isPendingClose() {
        return this.pendingClose && !this.closed;
    }

    /**
     * 检查写操作是否已禁用。
     * Check if write is disabled.
     *
     * @return 写是否禁用 / Whether write is disabled
     */
    protected final boolean isWriteDisabled() {
        return this.pendingClose || this.closed;
    }

    /**
     * 获取连接 IP。
     * Get connection IP.
     *
     * @return IP 地址 / IP address
     */
    public final String getIP() {
        return this.ip;
    }

    /**
     * 获取服务上下文标识。
     * Get service context identifier.
     *
     * @return 服务上下文 / Service context
     */
    public final String getServiceContext() {
        return this.serviceContext;
    }

    /**
     * 尝试锁定连接以独占处理包。
     * Try to lock the connection for exclusive packet processing.
     *
     * @return 是否锁定成功 / Whether lock was acquired
     */
    boolean tryLockConnection() {
        return !this.locked && (this.locked = true);
    }

    /**
     * 解锁连接。
     * Unlock the connection.
     */
    void unlockConnection() {
        this.locked = false;
    }

    /**
     * 处理接收到的数据帧。
     * Process received data frame.
     *
     * @param buf 数据缓冲 / Data buffer
     * @return 是否处理成功 / Whether processing succeeded
     */
    protected abstract boolean processData(ByteBuffer buf);

    /**
     * 向缓冲写出待发送数据。
     * Write pending data into the buffer.
     *
     * @param buf 写缓冲 / Write buffer
     * @return 是否还有后续数据 / Whether more data remains
     */
    protected abstract boolean writeData(ByteBuffer buf);

    /**
     * 连接初始化回调。
     * Connection initialization callback.
     */
    protected abstract void initialized();

    /**
     * 获取断开连接延迟（毫秒）。
     * Get disconnection delay in milliseconds.
     *
     * @return 延迟毫秒数 / Delay in milliseconds
     */
    protected abstract long getDisconnectionDelay();

    /**
     * 断开连接时的清理逻辑。
     * Cleanup logic on disconnection.
     */
    protected abstract void onDisconnect();

    /**
     * 服务器关闭时的处理。
     * Handle server shutdown.
     */
    protected abstract void onServerClose();
}
