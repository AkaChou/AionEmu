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
     * @param key 要检查的 short 键 / The short key to check
     * @return 存在则为 true / True if present
     */
    public boolean contains(short key) {
        return containsKey(key);
    }

    /**
     * 返回全部 short 键数组。
     * Return all keys as a short array.
     *
     * @return 全部键组成的数组 / Key array
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
