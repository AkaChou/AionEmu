package com.aionl.slf4j.conversion;

import ch.qos.logback.core.rolling.TriggeringPolicyBase;
import java.io.File;

/**
 * 简单启动触发策略类，用于控制日志文件的滚动
 * Simple startup triggering policy class for controlling log file rollover
 *
 * 该策略在应用程序启动时，如果日志文件已存在且不为空，则触发一次日志滚动
 * This policy triggers a log rollover once at application startup if the log file exists and is not empty
 *
 * @param <E> 日志事件类型 / Type of logging event
 */
public final class SimpleStartupTriggeringPolicy<E> extends TriggeringPolicyBase<E> {
    
    /**
     * 标记是否已触发过滚动
     * Flag indicating whether rollover has been triggered
     */
    private boolean fired = false;

    /**
     * 判断是否需要触发日志滚动
     * Determine if log rollover should be triggered
     *
     * @param activeFile 当前活动的日志文件 / Current active log file
     * @param event 日志事件 / Logging event
     * @return 如果需要触发滚动返回true，否则返回false / Returns true if rollover should be triggered, false otherwise
     */
    public boolean isTriggeringEvent(File activeFile, E event) {
        boolean result = !this.fired && activeFile.length() > 0L;
        this.fired = true;
        if (result) {
            this.addInfo("Triggering rollover for " + activeFile);
        }

        return result;
    }
}
