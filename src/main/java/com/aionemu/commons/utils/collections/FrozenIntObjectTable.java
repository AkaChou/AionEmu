package com.aionemu.commons.utils.collections;

import java.util.Arrays;
import java.util.function.IntFunction;

/**
 * 可冻结的 int 键只读表：构建期复用 {@link IntObjectHashMap} 真原始哈希写入，
 * {@link #freeze()} 后切换为「升序键数组 + 二分查找」的密集只读形态，
 * 适用于「加载期构建、运行期按键查询」且键集不再变化的密集表（如按地图 ID 索引的静态数据）。
 * Freezable int-keyed read-mostly table: writes go through the raw {@link IntObjectHashMap} during
 * build, and {@link #freeze()} switches to a dense "sorted key array + binary search" shape for
 * load-time-built, runtime-key-queried dense tables (e.g. static data indexed by map id).
 * <p>冻结后写操作抛 {@link IllegalStateException}（及早失败）；冻结前全部操作与
 * {@link IntObjectHashMap} 语义一致。内层嵌套结构（值对象内部的可变 Map）不参与冻结。</p>
 * <p>Writes after freeze throw {@link IllegalStateException} (fail-fast); before freeze every
 * operation keeps {@link IntObjectHashMap} semantics. Nested inner structures (mutable maps inside
 * the values) are not part of the freeze.</p>
 * @param <V> 值类型 / value type
 */
public final class FrozenIntObjectTable<V> {

    private IntObjectHashMap<V> builder;
    private int[] sortedKeys;
    private Object[] alignedValues;

    /**
     * 创建构建期表。
     * Creates a build-mode table.
     */
    public FrozenIntObjectTable() {
        this.builder = new IntObjectHashMap<>();
    }

    /**
     * 创建指定初始容量的构建期表。
     * Creates a build-mode table with the given initial capacity.
     * @param initialCapacity 初始容量 / initial capacity
     */
    public FrozenIntObjectTable(int initialCapacity) {
        this.builder = new IntObjectHashMap<>(initialCapacity);
    }

    /**
     * 冻结：键升序拷贝并对齐值数组，释放构建哈希；重复冻结为幂等 no-op。
     * Freeze: copy keys in ascending order with aligned values and release the build hash;
     * freezing twice is an idempotent no-op.
     */
    public synchronized void freeze() {
        if (sortedKeys != null) {
            return;
        }
        IntObjectHashMap<V> source = builder;
        if (source == null) {
            return;
        }
        int[] keys = source.keys();
        int[] sorted = Arrays.copyOf(keys, keys.length);
        Arrays.sort(sorted);
        Object[] values = new Object[sorted.length];
        for (int i = 0; i < sorted.length; i++) {
            values[i] = source.get(sorted[i]);
        }
        this.sortedKeys = sorted;
        this.alignedValues = values;
        this.builder = null;
    }

    /**
     * 是否已冻结。
     * Whether this table is frozen.
     * @return 已冻结为 true / true when frozen
     */
    public synchronized boolean isFrozen() {
        return sortedKeys != null;
    }

    /**
     * 按键取值；构建期走哈希，冻结后走二分；不存在返回 null。
     * Returns the value for the key; hashes while building, binary-searches once frozen;
     * null when absent.
     * @param key 键 / key
     * @return 值或 null / value or null
     */
    public V get(int key) {
        if (sortedKeys != null) {
            int index = Arrays.binarySearch(sortedKeys, key);
            return index >= 0 ? valueAt(index) : null;
        }
        return builder.get(key);
    }

    /**
     * 键是否存在；构建期走哈希，冻结后走二分。
     * Whether the key exists; hashes while building, binary-searches once frozen.
     * @param key 键 / key
     * @return 存在为 true / true when present
     */
    public boolean containsKey(int key) {
        if (sortedKeys != null) {
            return Arrays.binarySearch(sortedKeys, key) >= 0;
        }
        return builder.containsKey(key);
    }

    /**
     * 构建期写入；冻结后及早失败。
     * Puts during build mode; fails fast once frozen.
     * @param key 键 / key
     * @param value 值 / value
     * @return 旧值或 null / previous value or null
     */
    public V put(int key, V value) {
        requireBuildMode("put");
        return builder.put(key, value);
    }

    /**
     * 构建期不存在时写入；冻结后及早失败。
     * Puts if absent during build mode; fails fast once frozen.
     * @param key 键 / key
     * @param value 值 / value
     * @return 旧值或 null / previous value or null
     */
    public V putIfAbsent(int key, V value) {
        requireBuildMode("putIfAbsent");
        return builder.putIfAbsent(key, value);
    }

    /**
     * 构建期不存在时计算写入；冻结后及早失败。
     * Computes if absent during build mode; fails fast once frozen.
     * @param key 键 / key
     * @param mappingFunction 映射函数 / mapping function
     * @return 最终值 / resulting value
     */
    public V computeIfAbsent(int key, IntFunction<? extends V> mappingFunction) {
        requireBuildMode("computeIfAbsent");
        return builder.computeIfAbsent(key, mappingFunction);
    }

    /**
     * 构建期移除；冻结后及早失败。
     * Removes during build mode; fails fast once frozen.
     * @param key 键 / key
     * @return 旧值或 null / previous value or null
     */
    public V remove(int key) {
        requireBuildMode("remove");
        return builder.remove(key);
    }

    /**
     * 构建期清空；冻结后及早失败。
     * Clears during build mode; fails fast once frozen.
     */
    public void clear() {
        requireBuildMode("clear");
        builder.clear();
    }

    /**
     * 当前键值对数量；冻结后返回键数组长度。
     * Current entry count; the sorted key length once frozen.
     * @return 数量 / entry count
     */
    public synchronized int size() {
        return sortedKeys != null ? sortedKeys.length : builder.size();
    }

    /**
     * 是否为空。
     * Whether the table is empty.
     * @return 空为 true / true when empty
     */
    public synchronized boolean isEmpty() {
        return size() == 0;
    }

    /**
     * {@link #containsKey(int)} 的别名（兼容既有调用形态）。
     * Alias of {@link #containsKey(int)} kept for existing call shapes.
     * @param key 键 / key
     * @return 存在为 true / true when present
     */
    public boolean contains(int key) {
        return containsKey(key);
    }

    /**
     * 冻结后写操作统一及早失败。
     * Unified fail-fast guard for write operations after freeze.
     * @param operation 操作名 / operation name
     */
    private void requireBuildMode(String operation) {
        if (sortedKeys != null) {
            throw new IllegalStateException("FrozenIntObjectTable is frozen; " + operation + " is not allowed");
        }
    }

    /**
     * 读取对齐值（冻结期专用，无哈希路径）。
     * Reads the aligned value (frozen mode only, no hash path).
     * @param index 二分命中的下标 / index hit by binary search
     * @return 值 / value
     */
    @SuppressWarnings("unchecked")
    private V valueAt(int index) {
        return (V) alignedValues[index];
    }
}
