package com.aionemu.commons.callbacks;

/**
 * 回调优先级接口，用于定义回调执行的顺序
 * Interface for callback priority that defines the execution order of callbacks
 */
public interface CallbackPriority {
    /**
     * 默认优先级值
     * Default priority value
     */
    int DEFAULT_PRIORITY = 0;

    /**
     * 获取回调的优先级
     * Get the priority of the callback
     *
     * @return 优先级值，值越小优先级越高 / Priority value, lower value means higher priority
     */
    int getPriority();
}
