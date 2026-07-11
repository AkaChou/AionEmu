package com.aionemu.gameserver.lifecycle;

import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 启动日志计时网关：记录游戏服开始启动并返回起点时间戳。
 * Gateway for startup-log timing: logs game-server start and returns the epoch millis.
 */
@Component
@Slf4j
public class GameStartupLogGateway {

    /**
     * 记录启动开始日志并返回当前时间作为启动计时原点。
     * Log that the game server is starting and return the current time as the timing origin.
     *
     * @return 启动起点毫秒时间戳 / Startup epoch millis
     */
    public long start() {
        long startupTimeMillis = System.currentTimeMillis();
        log.info(I18n.get("console.startup.gameserver_starting"));
        return startupTimeMillis;
    }
}
