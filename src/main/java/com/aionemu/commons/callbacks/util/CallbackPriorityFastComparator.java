package com.aionemu.commons.callbacks.util;

import com.aionemu.commons.callbacks.Callback;
import javolution.util.FastComparator;

/**
 * 快速回调优先级比较器，继承自FastComparator以提供高性能的比较操作
 * Fast callback priority comparator that extends FastComparator for high-performance comparison operations
 *
 * 该类使用CallbackPriorityComparator来实现实际的优先级比较逻辑
 * This class uses CallbackPriorityComparator to implement the actual priority comparison logic
 */
public class CallbackPriorityFastComparator extends FastComparator<Callback<?>> {
    
    private static final long serialVersionUID = 5346780764438744817L;
    
    /**
     * 内部使用的回调优先级比较器实例
     * Internal callback priority comparator instance
     */
    private final CallbackPriorityComparator cpc = new CallbackPriorityComparator();

    /**
     * 获取回调对象的哈希码
     * Get the hash code of a callback object
     *
     * @param obj 要获取哈希码的回调对象 / The callback object to get hash code for
     * @return 回调对象的哈希码 / Hash code of the callback object
     */
    public int hashCodeOf(Callback<?> obj) {
        return obj.hashCode();
    }

    /**
     * 判断两个回调对象是否相等
     * Check if two callback objects are equal
     *
     * @param o1 第一个回调对象 / First callback object
     * @param o2 第二个回调对象 / Second callback object
     * @return 如果两个对象优先级相等则返回true，否则返回false
     *         Returns true if the priorities are equal, false otherwise
     */
    public boolean areEqual(Callback<?> o1, Callback<?> o2) {
        return this.cpc.compare(o1, o2) == 0;
    }

    /**
     * 比较两个回调对象的优先级
     * Compare the priorities of two callback objects
     *
     * @param o1 第一个回调对象 / First callback object
     * @param o2 第二个回调对象 / Second callback object
     * @return 优先级比较结果 / Priority comparison result
     */
    public int compare(Callback<?> o1, Callback<?> o2) {
        return this.cpc.compare(o1, o2);
    }
}
