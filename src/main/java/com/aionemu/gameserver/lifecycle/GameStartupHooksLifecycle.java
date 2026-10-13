package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.GameServer;
import org.springframework.stereotype.Component;

/**
 * 启动钩子阶段的生命周期：幂等执行启动钩子，加载状态由基类承载。
 * Lifecycle for startup hooks: idempotent hook execution; load state is hosted by the base class.
 */
@Component
public class GameStartupHooksLifecycle extends GameStartupStage {

    /**
     * 启动本阶段：运行游戏服启动钩子。
     * Start this stage: run game-server startup hooks.
     */
    @Override
    protected void runStage() {
        GameServer.runStartupHooks();
    }
}
