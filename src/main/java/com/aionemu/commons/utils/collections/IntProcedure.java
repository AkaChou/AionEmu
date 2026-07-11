package com.aionemu.commons.utils.collections;

/**
 * 处理 int 值的函数式过程；返回 false 可中断遍历。
 * Functional procedure over int values; return false to abort iteration.
 */
@FunctionalInterface
public interface IntProcedure {

    /**
     * 处理单个 int 值。
     * Process a single int value.
     *
     * Value
     * @return 继续遍历返回 true，中断返回 false / True to continue, false to stop
     */
    boolean execute(int value);
}
