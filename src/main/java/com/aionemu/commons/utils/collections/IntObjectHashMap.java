package com.aionemu.commons.utils.collections;

import java.util.LinkedHashMap;

/**
 * int→对象 哈希映射。
 * Int-to-object hash map.
 *
 * @param <V> 值类型 / Value type
 */
public class IntObjectHashMap<V> extends LinkedHashMap<Integer, V> {

    /**
     * 创建默认容量映射。
     * Create a map with default capacity.
     */
    public IntObjectHashMap() {
    }

    /**
     * 创建指定初始容量的映射。
     * Create a map with the given initial capacity.
     *
     * Initial capacity
     */
    public IntObjectHashMap(int initialCapacity) {
        super(initialCapacity);
    }

    /**
     * 是否包含 int 键。
     * Whether the map contains the int key.
     *
     * Key
     *
     * @param key 存在则为 true / True if present
     */
    public boolean contains(int key) {
        return containsKey(key);
    }

    /**
     * 返回全部 int 键数组。
     * Return all keys as an int array.
     *
     * Key array
     */
    public int[] keys() {
        return keySet().stream().mapToInt(Integer::intValue).toArray();
    }
}
