package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.GameServer;
import com.aionemu.gameserver.configs.main.GSConfig;
import org.springframework.stereotype.Component;

/**
 * 种族比例限制网关：判断开关并注册比例限制启动钩子。
 * Race-ratio-limit gateway: checks the switch and registers the ratio-limit startup hook.
 */
@Component
public class GameRatioLimitGateway {

    /**
     * 种族比例限制是否已启用。
     * Whether race-ratio limitation is enabled.
     *
     * @return 已启用为 {@code true} / {@code true} if enabled
     */
    public boolean isRatioLimitationEnabled() {
        return GSConfig.ENABLE_RATIO_LIMITATION;
    }

    /**
     * 注册种族比例限制启动钩子。
     * Register the race-ratio-limit startup hook.
     */
    public void registerRatioLimitStartupHook() {
        GameServer.registerRatioLimitStartupHook();
    }

    /**
     * 返回当前时间毫秒数。
     * Return the current time in milliseconds.
     *
     * @return 当前时间毫秒 / Current time millis
     */
    public long currentTimeMillis() {
        return System.currentTimeMillis();
    }
}
