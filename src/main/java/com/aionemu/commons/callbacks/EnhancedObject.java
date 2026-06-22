package com.aionemu.commons.callbacks;

import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 可增强对象接口，定义了对象的回调管理能力
 * Interface for enhanced objects that defines callback management capabilities
 */
public interface EnhancedObject {
    /**
     * 添加回调到对象
     * Add a callback to the object
     *
     * @param var1 要添加的回调 / Callback to add
     */
    void addCallback(Callback var1);

    /**
     * 从对象移除回调
     * Remove a callback from the object
     *
     * @param var1 要移除的回调 / Callback to remove
     */
    void removeCallback(Callback var1);

    /**
     * 获取对象的所有回调
     * Get all callbacks of the object
     *
     * @return 回调映射表，键为回调类型，值为回调列表 / Callback map with callback type as key and callback list as value
     */
    Map<Class<? extends Callback>, List<Callback>> getCallbacks();

    /**
     * 设置对象的回调映射表
     * Set the callback map of the object
     *
     * @param var1 回调映射表 / Callback map
     */
    void setCallbacks(Map<Class<? extends Callback>, List<Callback>> var1);

    /**
     * 获取回调操作的读写锁
     * Get the read-write lock for callback operations
     *
     * @return 读写锁实例 / Read-write lock instance
     */
    ReentrantReadWriteLock getCallbackLock();
}
