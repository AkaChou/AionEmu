package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.GameServer;
import com.aionemu.gameserver.configs.main.GSConfig;
import org.springframework.stereotype.Component;

/**
 * 种族比例限制生命周期：若启用则注册比例限制启动钩子，加载状态由基类承载。
 * Lifecycle for race-ratio limit: registers the ratio-limit startup hook when enabled; load state
 * is hosted by the base class.
 */
@Component
public class GameRatioLimitLifecycle extends GameStartupStage {

    /**
     * 启动本阶段：若启用则注册比例限制启动钩子。
     * Start this stage: register the ratio-limit startup hook when enabled.
     */
    @Override
    protected void runStage() {
        if (isRatioLimitationEnabled()) {
            registerRatioLimitStartupHook();
        }
    }

    /**
     * 种族比例限制是否已启用。
     * Whether race-ratio limitation is enabled.
     * @return 已启用为 {@code true} / {@code true} if enabled
     */
    protected boolean isRatioLimitationEnabled() {
        return GSConfig.ENABLE_RATIO_LIMITATION;
    }

    /**
     * 注册种族比例限制启动钩子。
     * Register the race-ratio-limit startup hook.
     */
    protected void registerRatioLimitStartupHook() {
        GameServer.registerRatioLimitStartupHook();
    }
}
