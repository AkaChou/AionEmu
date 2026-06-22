package com.aionemu.commons.callbacks.util;

import com.aionemu.commons.callbacks.Callback;
import java.util.Comparator;

/**
 * 回调优先级比较器，用于对回调对象进行优先级排序
 * Callback priority comparator for sorting callback objects based on their priority
 *
 * 实现了Comparator接口，通过比较两个回调对象的优先级值来确定它们的顺序
 * Implements Comparator interface to determine the order of two callbacks by comparing their priority values
 */
public class CallbackPriorityComparator implements Comparator<Callback<?>> {

    /**
     * 比较两个回调对象的优先级
     * Compare the priorities of two callback objects
     *
     * @param o1 第一个回调对象 / First callback object
     * @param o2 第二个回调对象 / Second callback object
     * @return 负数表示o1优先级高于o2，0表示优先级相等，正数表示o1优先级低于o2
     *         Negative if o1 has higher priority, 0 if equal, positive if o1 has lower priority
     */
    public int compare(Callback<?> o1, Callback<?> o2) {
        int p1 = CallbacksUtil.getCallbackPriority(o1);
        int p2 = CallbacksUtil.getCallbackPriority(o2);
        if (p1 < p2) {
            return -1;
        } else {
            return p1 == p2 ? 0 : 1;
        }
    }
}
