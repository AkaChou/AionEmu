package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.GameServer;
import org.springframework.stereotype.Component;

/**
 * 启动钩子网关：委托 {@link GameServer#runStartupHooks()} 执行注册钩子。
 * Startup-hooks gateway: delegates to {@link GameServer#runStartupHooks()}.
 */
@Component
public class GameStartupHooksGateway {

    /**
     * 运行游戏服启动钩子。
     * Run game-server startup hooks.
     */
    public void start() {
        GameServer.runStartupHooks();
    }
}
