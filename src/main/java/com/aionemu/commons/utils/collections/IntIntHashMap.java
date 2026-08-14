package com.aionemu.commons.utils.collections;

import java.util.LinkedHashMap;

/**
 * int→int 哈希映射，缺失键时返回 0。
 * Int-to-int hash map that returns 0 for missing keys.
 */
public class IntIntHashMap extends LinkedHashMap<Integer, Integer> {

    /**
     * 按 int 键取值，缺失返回 0。
     * Get by int key, returning 0 when missing.
     *
     * @param key 要查询的 int 键 / The int key to query
     * @return 键对应的值，缺失为 0 / Value for the key, 0 when missing
     */
    public int get(int key) {
        Integer value = super.get(key);
        return value == null ? 0 : value;
    }

    /**
     * 按 int 键写入，返回旧值（缺失为 0）。
     * Put by int key, returning the previous value (0 if none).
     *
     * @param key 要写入的 int 键 / The int key to store
     * @param value 要存储的值 / The value to store
     * @return 旧值，无则为 0 / Previous value or 0
     */
    public int put(int key, int value) {
        Integer previous = super.put(key, value);
        return previous == null ? 0 : previous;
    }

    /**
     * 是否包含 int 键。
     * Whether the map contains the int key.
     *
     * @param key 要检查的 int 键 / The int key to check
     * @return 存在则为 true / True if present
     */
    public boolean contains(int key) {
        return containsKey(key);
    }
}
