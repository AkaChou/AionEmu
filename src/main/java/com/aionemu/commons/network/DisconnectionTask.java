package com.aionemu.commons.network;

/**
 * 断开连接任务类
 * Disconnection Task Class
 *
 * 该类负责执行连接断开时的清理工作
 * This class is responsible for executing cleanup work when a connection is disconnected
 */
public class DisconnectionTask implements Runnable {
    
    /**
     * 要断开的连接实例
     * The connection instance to be disconnected
     */
    private final AConnection connection;

    /**
     * 构造函数
     * Constructor
     *
     * @param connection 要断开的连接 / The connection to disconnect
     */
    public DisconnectionTask(AConnection connection) {
        this.connection = connection;
    }

    /**
     * 执行断开连接的操作
     * Execute the disconnection operation
     */
    @Override
    public void run() {
        this.connection.onDisconnect();
    }
}
