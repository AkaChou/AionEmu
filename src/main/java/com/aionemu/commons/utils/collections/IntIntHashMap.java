package com.aionemu.commons.utils.collections;

import java.io.Serial;
import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * 原始 int 键 → 原始 int 值的开放寻址哈希映射（缺失键时默认返回 0）。
 * Open-addressing primitive int-to-int hash map returning 0 for missing keys.
 * <p>底层由连续扁平数组驱动，消除双重装箱开销，同时保持 {@link Map} 集合契约兼容。
 * Driven by flat primitive arrays to eliminate dual-boxing overhead while maintaining {@link Map} contract compatibility.</p>
 */
public class IntIntHashMap extends AbstractMap<Integer, Integer> implements Cloneable, Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * 默认初始容量（2 的幂）。 / Default initial capacity (power of two).
	 */
	private static final int DEFAULT_CAPACITY = 16;

	/**
	 * 最小容量（2 的幂）。 / Minimum capacity (power of two).
	 */
	private static final int MIN_CAPACITY = 8;

	/**
	 * 扩容负载因子。 / Load factor for the resize threshold.
	 */
	private static final float LOAD_FACTOR = 0.75f;

	private int[] keys;
	private int[] values;
	private byte[] used;
	private int mask;
	private int size;
	private int resizeThreshold;

	/**
	 * 创建默认容量的映射。
	 * Creates a map with default capacity.
	 */
	public IntIntHashMap() {
		this(DEFAULT_CAPACITY);
	}

	/**
	 * 创建指定初始容量的映射。
	 * Creates a map sized to hold the given initial capacity without immediate resizing.
	 *
	 * @param initialCapacity 初始期望容量 / initial expected capacity
	 */
	public IntIntHashMap(int initialCapacity) {
		if (initialCapacity < 0) {
			throw new IllegalArgumentException("Illegal initial capacity: " + initialCapacity);
		}
		int cap = (int) Math.ceil(initialCapacity / LOAD_FACTOR);
		init(Math.max(MIN_CAPACITY, tableSizeFor(cap)));
	}

	/**
	 * 基于已有映射内容创建新映射。
	 * Creates a new map with the same mappings as the specified map.
	 *
	 * @param m 初始数据映射 / initial map
	 */
	public IntIntHashMap(Map<? extends Integer, ? extends Integer> m) {
		this(m != null ? m.size() : DEFAULT_CAPACITY);
		if (m != null) {
			putAll(m);
		}
	}

	/**
	 * 按原始 int 键取值，缺失时返回 0。
	 * Gets value by primitive int key, returning 0 when missing.
	 *
	 * @param key 要查询的 int 键 / the int key to query
	 * @return 对应值，缺失时返回 0 / value for the key, 0 when missing
	 */
	public int get(int key) {
		int index = indexOf(key);
		return used[index] != 0 ? values[index] : 0;
	}

	/**
	 * 按原始 int 键写入，返回旧值（缺失时为 0）。
	 * Puts by primitive int key, returning previous value (0 if none).
	 *
	 * @param key   要写入的 int 键 / the int key to store
	 * @param value 要存储的 int 值 / the int value to store
	 * @return 旧值，无则为 0 / previous value or 0
	 */
	public int put(int key, int value) {
		int index = indexOf(key);
		if (used[index] != 0) {
			int previous = values[index];
			values[index] = value;
			return previous;
		}
		insert(index, key, value);
		return 0;
	}

	/**
	 * 移除原始 int 键，返回被移除的值（缺失时为 0）。
	 * Removes primitive int key, returning removed value (0 if none).
	 *
	 * @param key 要移除的 int 键 / the int key to remove
	 * @return 被移除的值或 0 / removed value or 0
	 */
	public int remove(int key) {
		int index = indexOf(key);
		if (used[index] == 0) {
			return 0;
		}
		int previous = values[index];
		size--;
		shiftKeys(index);
		return previous;
	}

	/**
	 * 是否包含指定 int 键。
	 * Whether the map contains the int key.
	 *
	 * @param key 要检查的 int 键 / the int key to check
	 * @return 存在返回 true / true if present
	 */
	public boolean containsKey(int key) {
		return used[indexOf(key)] != 0;
	}

	/**
	 * 是否包含指定 int 键（containsKey 别名）。
	 * Whether the map contains the int key (alias of containsKey).
	 *
	 * @param key 要检查的 int 键 / the int key to check
	 * @return 存在返回 true / true if present
	 */
	public boolean contains(int key) {
		return containsKey(key);
	}

	/**
	 * 返回全部 int 键的原生数组。
	 * Returns an array containing all primitive int keys.
	 *
	 * @return 包含全部键的数组 / array of all keys
	 */
	public int[] keys() {
		int[] result = new int[size];
		int index = 0;
		for (int i = 0; i < used.length; i++) {
			if (used[i] != 0) {
				result[index++] = keys[i];
			}
		}
		return result;
	}

	/**
	 * 返回全部 int 值的原生数组。
	 * Returns an array containing all primitive int values.
	 *
	 * @return 包含全部值的数组 / array of all values
	 */
	public int[] valuesArray() {
		int[] result = new int[size];
		int index = 0;
		for (int i = 0; i < used.length; i++) {
			if (used[i] != 0) {
				result[index++] = values[i];
			}
		}
		return result;
	}

	/**
	 * 原始 int-int 键值消费函数式接口。
	 * Functional interface for consuming primitive int key and value.
	 */
	@FunctionalInterface
	public interface IntIntConsumer {
		/**
		 * 接收键值对。
		 * Accepts key and value.
		 *
		 * @param key   int 键 / int key
		 * @param value int 值 / int value
		 */
		void accept(int key, int value);
	}

	/**
	 * 遍历全部键值对（零装箱）。
	 * Performs the given action for all key-value pairs without boxing.
	 *
	 * @param action 遍历动作 / action
	 */
	public void forEachEntry(IntIntConsumer action) {
		for (int i = 0; i < used.length; i++) {
			if (used[i] != 0) {
				action.accept(keys[i], values[i]);
			}
		}
	}

	// ==================== Map 接口兼容实现 / Map Interface Compatibility ====================

	@Override
	public Integer get(Object key) {
		if (key instanceof Integer k) {
			int index = indexOf(k);
			return used[index] != 0 ? values[index] : null;
		}
		return null;
	}

	@Override
	public Integer put(Integer key, Integer value) {
		if (key == null || value == null) {
			throw new NullPointerException("IntIntHashMap does not permit null keys or values");
		}
		int k = key;
		int v = value;
		int index = indexOf(k);
		if (used[index] != 0) {
			int previous = values[index];
			values[index] = v;
			return previous;
		}
		insert(index, k, v);
		return null;
	}

	@Override
	public boolean containsKey(Object key) {
		if (key instanceof Integer k) {
			return containsKey(k.intValue());
		}
		return false;
	}

	@Override
	public Integer remove(Object key) {
		if (key instanceof Integer k) {
			int index = indexOf(k);
			if (used[index] == 0) {
				return null;
			}
			int previous = values[index];
			size--;
			shiftKeys(index);
			return previous;
		}
		return null;
	}

	@Override
	public boolean containsValue(Object value) {
		if (value instanceof Integer v) {
			int primitiveVal = v;
			for (int i = 0; i < used.length; i++) {
				if (used[i] != 0 && values[i] == primitiveVal) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public int size() {
		return size;
	}

	@Override
	public boolean isEmpty() {
		return size == 0;
	}

	@Override
	public void clear() {
		if (size == 0) {
			return;
		}
		Arrays.fill(used, (byte) 0);
		size = 0;
	}

	@Override
	public void forEach(BiConsumer<? super Integer, ? super Integer> action) {
		for (int i = 0; i < used.length; i++) {
			if (used[i] != 0) {
				action.accept(keys[i], values[i]);
			}
		}
	}

	@Override
	public Set<Integer> keySet() {
		return new AbstractSet<>() {
			@Override
			public Iterator<Integer> iterator() {
				return new Iterator<>() {
					private int nextIdx = advance(0);

					private int advance(int from) {
						while (from < used.length && used[from] == 0) {
							from++;
						}
						return from;
					}

					@Override
					public boolean hasNext() {
						return nextIdx < used.length;
					}

					@Override
					public Integer next() {
						if (!hasNext()) {
							throw new NoSuchElementException();
						}
						int idx = nextIdx;
						nextIdx = advance(nextIdx + 1);
						return keys[idx];
					}
				};
			}

			@Override
			public int size() {
				return IntIntHashMap.this.size();
			}

			@Override
			public boolean contains(Object o) {
				return IntIntHashMap.this.containsKey(o);
			}

			@Override
			public boolean remove(Object o) {
				return IntIntHashMap.this.remove(o) != null;
			}

			@Override
			public void clear() {
				IntIntHashMap.this.clear();
			}
		};
	}

	@Override
	public Collection<Integer> values() {
		return new AbstractCollection<>() {
			@Override
			public Iterator<Integer> iterator() {
				return new Iterator<>() {
					private int nextIdx = advance(0);

					private int advance(int from) {
						while (from < used.length && used[from] == 0) {
							from++;
						}
						return from;
					}

					@Override
					public boolean hasNext() {
						return nextIdx < used.length;
					}

					@Override
					public Integer next() {
						if (!hasNext()) {
							throw new NoSuchElementException();
						}
						int idx = nextIdx;
						nextIdx = advance(nextIdx + 1);
						return values[idx];
					}
				};
			}

			@Override
			public int size() {
				return IntIntHashMap.this.size();
			}

			@Override
			public boolean contains(Object o) {
				return IntIntHashMap.this.containsValue(o);
			}

			@Override
			public void clear() {
				IntIntHashMap.this.clear();
			}
		};
	}

	@Override
	public Set<Entry<Integer, Integer>> entrySet() {
		return new AbstractSet<>() {
			@Override
			public Iterator<Entry<Integer, Integer>> iterator() {
				return new Iterator<>() {
					private int nextIdx = advance(0);

					private int advance(int from) {
						while (from < used.length && used[from] == 0) {
							from++;
						}
						return from;
					}

					@Override
					public boolean hasNext() {
						return nextIdx < used.length;
					}

					@Override
					public Entry<Integer, Integer> next() {
						if (!hasNext()) {
							throw new NoSuchElementException();
						}
						int idx = nextIdx;
						nextIdx = advance(nextIdx + 1);
						return new AbstractMap.SimpleImmutableEntry<>(keys[idx], values[idx]);
					}
				};
			}

			@Override
			public int size() {
				return IntIntHashMap.this.size();
			}

			@Override
			public void clear() {
				IntIntHashMap.this.clear();
			}
		};
	}

	@Override
	public IntIntHashMap clone() {
		try {
			IntIntHashMap clone = (IntIntHashMap) super.clone();
			clone.keys = keys.clone();
			clone.values = values.clone();
			clone.used = used.clone();
			return clone;
		} catch (CloneNotSupportedException e) {
			throw new AssertionError(e);
		}
	}

	// ==================== 内部开地址散列引擎 / Internal Open-Addressing Engine ====================

	private int indexOf(int key) {
		int index = spread(key) & mask;
		while (used[index] != 0 && keys[index] != key) {
			index = (index + 1) & mask;
		}
		return index;
	}

	private void insert(int index, int key, int value) {
		keys[index] = key;
		values[index] = value;
		used[index] = 1;
		if (++size >= resizeThreshold) {
			resize();
		}
	}

	private void shiftKeys(int pos) {
		int last = pos;
		while (true) {
			pos = (pos + 1) & mask;
			if (used[pos] == 0) {
				used[last] = 0;
				return;
			}
			int slot = spread(keys[pos]) & mask;
			if (last <= pos ? (last < slot && slot <= pos) : (last < slot || slot <= pos)) {
				continue;
			}
			keys[last] = keys[pos];
			values[last] = values[pos];
			last = pos;
		}
	}

	private void resize() {
		int[] oldKeys = keys;
		int[] oldValues = values;
		byte[] oldUsed = used;
		init(oldKeys.length << 1);
		for (int i = 0; i < oldUsed.length; i++) {
			if (oldUsed[i] != 0) {
				int index = indexOf(oldKeys[i]);
				keys[index] = oldKeys[i];
				values[index] = oldValues[i];
				used[index] = 1;
				size++;
			}
		}
	}

	private void init(int capacity) {
		keys = new int[capacity];
		values = new int[capacity];
		used = new byte[capacity];
		mask = capacity - 1;
		size = 0;
		resizeThreshold = Math.max(MIN_CAPACITY, (int) (capacity * LOAD_FACTOR));
	}

	private static int spread(int key) {
		int h = key * 0x9E3779B9;
		return h ^ (h >>> 16);
	}

	private static int tableSizeFor(int cap) {
		int n = cap - 1;
		n |= n >>> 1;
		n |= n >>> 2;
		n |= n >>> 4;
		n |= n >>> 8;
		n |= n >>> 16;
		return (n < 0) ? 1 : (n >= 1 << 30) ? 1 << 30 : n + 1;
	}
}
