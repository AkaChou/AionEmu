package com.aionemu.commons.utils.collections;

import java.util.Arrays;

/**
 * 原始 long 键 → 对象的开放寻址哈希表（热路径专用，避免 {@link Long} 装箱）。
 * Open-addressing long-to-object hash map for hot paths (avoids {@link Long} boxing).
 * <p>与 {@code HashMap<Long, V>} 的关键差异 / Key differences from {@code HashMap<Long, V>}：
 * 键为原始 {@code long}（int 键可直接传入，不会装箱）、不支持 {@code null} 值以外的容器语义、
 * 无迭代能力（本表只用于逐节点 put/get/clear 的场景）。
 * Keys are primitives (int keys widen implicitly without boxing), there is no iteration support,
 * and a stored {@code null} value is indistinguishable from "absent" via {@link #get(long)},
 * matching {@link java.util.HashMap#get(Object)} semantics.</p>
 * <p>非线程安全；调用方负责独占使用（如各搜索线程自己的 workspace）。
 * Not thread-safe; the caller owns the instance (e.g. a per-thread search workspace).</p>
 * @param <V> 值类型 / value type
 */
public final class LongObjectHashMap<V> {

    /** 扩容阈值负载因子。 / Load factor for the resize threshold. */
    private static final float LOAD_FACTOR = 0.75f;

    /** 最小容量（2 的幂）。 / Minimum capacity (power of two). */
    private static final int MIN_CAPACITY = 8;

    /**
     * 当前占用的槽位下标（按需增长），让 {@link #clear()} 的代价与条目数成正比而不是与容量成正比。
     * Indices of the currently used slots (grown on demand) so {@link #clear()} costs the entry count
     * rather than the whole table capacity — the A* workspace keeps a 16k-slot table alive and clears
     * it once per search.
     */
    private int[] touched;
    private int touchedCount;
    private long[] keys;
    private Object[] values;
    private byte[] used;
    private int mask;
    private int size;
    private int resizeThreshold;

    /**
     * 创建默认容量（8）的映射。
     * Creates a map with the default capacity (8).
     */
    public LongObjectHashMap() {
        this(MIN_CAPACITY);
    }

    /**
     * 创建可容纳期望条目数而不立即扩容的映射。
     * Creates a map sized to hold the expected entry count without immediate resizing.
     * @param expectedSize 期望条目数 / expected number of entries
     */
    public LongObjectHashMap(int expectedSize) {
        int capacity = MIN_CAPACITY;
        while (capacity * LOAD_FACTOR < expectedSize) {
            capacity <<= 1;
        }
        init(capacity);
    }

    /**
     * 读取键对应的值；不存在返回 {@code null}。
     * Returns the value for the key, or {@code null} when absent.
     * @param key 原始 long 键 / primitive long key
     * @return 值或 null / value or null
     */
    @SuppressWarnings("unchecked")
    public V get(long key) {
        int index = indexOf(key);
        return used[index] != 0 ? (V) values[index] : null;
    }

    /**
     * 写入键值对，返回被覆盖的旧值。
     * Stores a key/value pair and returns the previous value.
     * @param key 原始 long 键 / primitive long key
     * @param value 值 / value
     * @return 旧值或 null / previous value or null
     */
    @SuppressWarnings("unchecked")
    public V put(long key, V value) {
        int index = indexOf(key);
        if (used[index] != 0) {
            V previous = (V) values[index];
            values[index] = value;
            return previous;
        }
        insert(index, key, value);
        return null;
    }

    /**
     * 仅当键不存在时写入，返回已存在的值（存在时）或 {@code null}。
     * Stores only when the key is absent; returns the existing value, or {@code null} when inserted.
     * @param key 原始 long 键 / primitive long key
     * @param value 值 / value
     * @return 已存在的值或 null / existing value or null
     */
    @SuppressWarnings("unchecked")
    public V putIfAbsent(long key, V value) {
        int index = indexOf(key);
        if (used[index] != 0) {
            return (V) values[index];
        }
        insert(index, key, value);
        return null;
    }

    /**
     * 返回当前条目数。
     * Returns the current entry count.
     * @return 条目数 / entry count
     */
    public int size() {
        return size;
    }

    /**
     * 清空全部条目并保留容量，便于搜索工作区复用。
     * Clears all entries while keeping capacity, so search workspaces can be reused.
     */
    public void clear() {
        if (size == 0) {
            return;
        }
        // 只回退真正被占用的槽位：容量可能是按搜索预算预分配的（例如 16384 槽只放了 300 条）。
        // Only the slots that were actually taken are reset: the capacity may be pre-sized from a search
        // budget (e.g. 16384 slots holding 300 entries).
        for (int i = 0; i < touchedCount; i++) {
            int slot = touched[i];
            used[slot] = 0;
            values[slot] = null;
        }
        touchedCount = 0;
        size = 0;
    }

    /**
     * 预分配到可容纳期望条目数，避免首次深搜时反复扩容搬移底层数组。
     * Pre-sizes the table for the expected entry count so a first deep search does not resize repeatedly.
     * <p>调用方必须保证当前表为空（例如搜索开始前刚执行过 {@link #clear()}），因为该方法会重建底层数组。
     * The caller must guarantee the map is empty (for example right after {@link #clear()}), because the backing
     * arrays are rebuilt.</p>
     * @param expectedEntries 期望条目数 / expected entry count
     */
    public void ensureCapacity(int expectedEntries) {
        int capacity = MIN_CAPACITY;
        while (capacity * LOAD_FACTOR < expectedEntries) {
            capacity <<= 1;
        }
        if (capacity > keys.length) {
            init(capacity);
        }
    }

    /**
     * 定位键所在槽位（命中返回该槽，未命中返回第一个空槽）。
     * Locates the slot for the key (existing slot on hit, first free slot on miss).
     * @param key 原始 long 键 / primitive long key
     * @return 槽位下标 / slot index
     */
    private int indexOf(long key) {
        int index = spread(key) & mask;
        while (used[index] != 0 && keys[index] != key) {
            index = (index + 1) & mask;
        }
        return index;
    }

    private void insert(int index, long key, V value) {
        keys[index] = key;
        values[index] = value;
        used[index] = 1;
        recordSlot(index);
        if (++size >= resizeThreshold) {
            resize();
        }
    }

    /**
     * 记录一个新占用的槽位下标（超出当前数组时按需扩容）。
     * Records a newly taken slot index, growing the bookkeeping array on demand.
     * @param index 槽位下标 / slot index
     */
    private void recordSlot(int index) {
        if (touchedCount == touched.length) {
            touched = Arrays.copyOf(touched, Math.max(MIN_CAPACITY, touched.length << 1));
        }
        touched[touchedCount++] = index;
    }

    private void resize() {
        long[] oldKeys = keys;
        Object[] oldValues = values;
        byte[] oldUsed = used;
        init(keys.length << 1);
        for (int i = 0; i < oldUsed.length; i++) {
            if (oldUsed[i] != 0) {
                int index = indexOf(oldKeys[i]);
                keys[index] = oldKeys[i];
                values[index] = oldValues[i];
                used[index] = 1;
                recordSlot(index);
                size++;
            }
        }
    }

    private void init(int capacity) {
        touched = new int[MIN_CAPACITY];
        touchedCount = 0;
        keys = new long[capacity];
        values = new Object[capacity];
        used = new byte[capacity];
        mask = capacity - 1;
        size = 0;
        resizeThreshold = Math.max(MIN_CAPACITY, (int) (capacity * LOAD_FACTOR));
    }

    /**
     * 64 位键的混淆散列（Fibonacci 乘法 + 高低位异或）。
     * Spreads 64-bit keys (Fibonacci multiply plus high/low xor).
     * @param key 原始 long 键 / primitive long key
     * @return 散列值 / hash value
     */
    private static int spread(long key) {
        long hash = key * 0x9E3779B97F4A7C15L;
        return (int) (hash ^ (hash >>> 32));
    }
}
