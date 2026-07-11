package com.aionemu.gameserver.lifecycle;

import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 启动完成日志网关：输出初始化完成、总耗时与就绪信息。
 * Gateway for startup-completion logging: init complete, total time, and ready messages.
 */
@Component
@Slf4j
public class GameStartupCompletionGateway {

    /**
     * 记录启动完成相关日志。
     * Log startup-completion messages.
     *
     * @param startupTime 总初始化耗时（毫秒） / Total initialization time in milliseconds
     */
    public void logStartupComplete(long startupTime) {
        log.info(I18n.get("console.startup.init_complete"));
        log.info(I18n.get("console.startup.total_init_time", startupTime));
        log.info(I18n.get("console.startup.ready"));
    }

    /**
     * 当前时间毫秒数（便于测试替换）。
     * Current time in milliseconds (overridable for tests).
     *
     * @return 当前毫秒时间戳 / Current epoch millis
     */
    public long currentTimeMillis() {
        return System.currentTimeMillis();
    }
}
