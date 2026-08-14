package com.aionemu.commons.utils.collections;

import java.util.ArrayList;
import java.util.Collection;

/**
 * 基于 {@link ArrayList} 的 int 友好列表。
 * Int-friendly list backed by {@link ArrayList}.
 */
public class IntArrayList extends ArrayList<Integer> {

    /**
     * 创建空列表。
     * Create an empty list.
     */
    public IntArrayList() {
    }

    /**
     * 用给定集合初始化。
     * Initialize with the given collection.
     *
     * @param values 初始值集合 / Initial values
     */
    public IntArrayList(Collection<Integer> values) {
        super(values);
    }

    /**
     * 添加 int 值。
     * Add an int value.
     *
     * @param value 要添加的 int 值 / The int value to add
     * @return 是否添加成功 / Whether added
     */
    public boolean add(int value) {
        return super.add(value);
    }

    /**
     * 是否包含 int 值。
     * Whether the list contains the int value.
     *
     * @param value 要检查的 int 值 / The int value to check
     * @return 存在则为 true / True if present
     */
    public boolean contains(int value) {
        return super.contains(value);
    }

    /**
     * 遍历元素；过程返回 false 时提前结束。
     * Iterate elements; stop early when the procedure returns false.
     *
     * @param procedure 回调过程 / Callback procedure
     * @return 全部执行完为 true，提前中断为 false / True if completed, false if aborted
     */
    public boolean forEach(IntProcedure procedure) {
        for (int value : this) {
            if (!procedure.execute(value)) {
                return false;
            }
        }
        return true;
    }
}
