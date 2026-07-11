package com.aionemu.commons.network;

import com.aionemu.commons.services.ServiceContext;
import lombok.RequiredArgsConstructor;

/**
 * 断开连接任务，在正确服务上下文中执行清理。
 * Disconnection task that runs cleanup in the correct service context.
 */
@RequiredArgsConstructor
public class DisconnectionTask implements Runnable {

    /**
     * 要断开的连接实例。
     * Connection instance to disconnect.
     */
    private final AConnection connection;

    /**
     * 执行断开连接清理。
     * Execute disconnection cleanup.
     */
    @Override
    public void run() {
        try (ServiceContext.Scope ignored = ServiceContext.use(connection.getServiceContext())) {
            this.connection.onDisconnect();
        }
    }
}
