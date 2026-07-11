package com.aionemu.commons.network;

/**
 * 连接传输层抽象，封装底层通道的 IP、写兴趣与关闭。
 * Connection transport abstraction for IP, write interest, and close.
 */
public interface ConnectionTransport {

    /**
     * 获取对端 IP。
     * Get remote IP.
     *
     * IP address
     */
    String getIP();

    /**
     * 启用写兴趣并触发刷写。
     * Enable write interest and flush pending writes.
     */
    void enableWriteInterest();

    /**
     * 关闭连接。
     * Close the connection.
     *
     * @param forced 是否强制关闭 / Whether to force close
     */
    void close(boolean forced);

    /**
     * 仅关闭底层资源（幂等）。
     * Close underlying resources only (idempotent).
     *
     * @return 是否实际关闭 / Whether close was performed
     */
    boolean onlyClose();
}
