package com.aionemu.commons.utils.collections;

import java.util.LinkedHashMap;

/**
 * short→对象 哈希映射。
 * Short-to-object hash map.
 *
 * @param <V> 值类型 / Value type
 */
public class ShortObjectHashMap<V> extends LinkedHashMap<Short, V> {

    /**
     * 是否包含 short 键。
     * Whether the map contains the short key.
     *
     * Key
     *
     * @param key 存在则为 true / True if present
     */
    public boolean contains(short key) {
        return containsKey(key);
    }

    /**
     * 返回全部 short 键数组。
     * Return all keys as a short array.
     *
     * Key array
     */
    public short[] keys() {
        short[] keys = new short[size()];
        int index = 0;
        for (short key : keySet()) {
            keys[index++] = key;
        }
        return keys;
    }
}
