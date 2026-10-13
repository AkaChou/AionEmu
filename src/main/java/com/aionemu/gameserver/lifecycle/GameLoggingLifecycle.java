package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.GameServer;
import org.springframework.stereotype.Component;

/**
 * 日志初始化生命周期：触发游戏服务器日志初始化，加载状态由基类承载。
 * Lifecycle for logging: triggers game-server logger initialization; load state is hosted by the
 * base class.
 */
@Component
public class GameLoggingLifecycle extends GameStartupStage {

    /**
     * 启动本阶段：初始化日志系统。
     * Start this stage: initialize the logging system.
     */
    @Override
    protected void runStage() {
        GameServer.initializeLogger();
    }
}
