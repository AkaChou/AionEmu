package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.GameServer;
import org.springframework.stereotype.Component;

/**
 * 日志初始化网关：触发游戏服务器日志初始化。
 * Logging gateway: triggers game-server logger initialization.
 */
@Component
public class GameLoggingGateway {

    /**
     * 启动日志初始化。
     * Start logging initialization.
     */
    public void start() {
        GameServer.initializeLogger();
    }
}
