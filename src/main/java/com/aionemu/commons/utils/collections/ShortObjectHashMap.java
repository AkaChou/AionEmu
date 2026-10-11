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
 * 原始 short 键 → 对象的开放寻址哈希表（避免 {@link Short} 装箱与 Entry 节点分配）。
 * Open-addressing short-to-object hash map (avoids {@link Short} boxing and Entry node allocation).
 * <p>底层由紧凑扁平数组支持，消除所有原生装箱开销，同时完整实现 {@link Map} 接口以保持无缝兼容。
 * Backed by compact flat arrays, eliminating boxing overhead while fully implementing {@link Map} for compatibility.</p>
 *
 * @param <V> 值类型 / Value type
 */
public class ShortObjectHashMap<V> extends AbstractMap<Short, V> implements Cloneable, Serializable {

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

	private short[] keys;
	private Object[] values;
	private byte[] used;
	private int mask;
	private int size;
	private int resizeThreshold;

	/**
	 * 创建默认容量的映射。
	 * Creates a map with default capacity.
	 */
	public ShortObjectHashMap() {
		this(DEFAULT_CAPACITY);
	}

	/**
	 * 创建指定初始容量的映射。
	 * Creates a map sized to hold the given initial capacity without immediate resizing.
	 *
	 * @param initialCapacity 初始期望容量 / initial expected capacity
	 */
	public ShortObjectHashMap(int initialCapacity) {
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
	public ShortObjectHashMap(Map<? extends Short, ? extends V> m) {
		this(m != null ? m.size() : DEFAULT_CAPACITY);
		if (m != null) {
			putAll(m);
		}
	}

	/**
	 * 按原始 short 键查询值；不存在时返回 null。
	 * Gets the value for the primitive short key, or null if absent.
	 *
	 * @param key short 键 / short key
	 * @return 对应值或 null / mapped value or null
	 */
	@SuppressWarnings("unchecked")
	public V get(short key) {
		int index = indexOf(key);
		return used[index] != 0 ? (V) values[index] : null;
	}

	/**
	 * 写入原始 short 键值对，返回旧值。
	 * Puts a key/value pair with a primitive short key, returning previous value.
	 *
	 * @param key   short 键 / short key
	 * @param value 值 / value
	 * @return 旧值或 null / previous value or null
	 */
	@SuppressWarnings("unchecked")
	public V put(short key, V value) {
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
	 * 仅当键不存在时写入。
	 * Puts the value only if the key is not already present.
	 *
	 * @param key   short 键 / short key
	 * @param value 值 / value
	 * @return 已存在的值，或刚写入成功时返回 null / existing value, or null if inserted
	 */
	@SuppressWarnings("unchecked")
	public V putIfAbsent(short key, V value) {
		int index = indexOf(key);
		if (used[index] != 0) {
			return (V) values[index];
		}
		insert(index, key, value);
		return null;
	}

	/**
	 * 移除原始 short 键，返回被移除的旧值。
	 * Removes the primitive short key, returning previous value or null.
	 *
	 * @param key short 键 / short key
	 * @return 被移除的值或 null / removed value or null
	 */
	@SuppressWarnings("unchecked")
	public V remove(short key) {
		int index = indexOf(key);
		if (used[index] == 0) {
			return null;
		}
		V previous = (V) values[index];
		size--;
		shiftKeys(index);
		return previous;
	}

	/**
	 * 是否包含指定 short 键。
	 * Whether the map contains the short key.
	 *
	 * @param key short 键 / short key
	 * @return 存在返回 true / true if present
	 */
	public boolean containsKey(short key) {
		return used[indexOf(key)] != 0;
	}

	/**
	 * 是否包含指定 short 键（containsKey 别名）。
	 * Whether the map contains the short key (alias of containsKey).
	 *
	 * @param key short 键 / short key
	 * @return 存在返回 true / true if present
	 */
	public boolean contains(short key) {
		return containsKey(key);
	}

	/**
	 * 返回全部 short 键的原生数组。
	 * Returns an array containing all primitive short keys.
	 *
	 * @return 包含全部键的数组 / array of all keys
	 */
	public short[] keys() {
		short[] result = new short[size];
		int index = 0;
		for (int i = 0; i < used.length; i++) {
			if (used[i] != 0) {
				result[index++] = keys[i];
			}
		}
		return result;
	}

	// ==================== Map 接口兼容实现 / Map Interface Compatibility ====================

	@Override
	public V get(Object key) {
		if (key instanceof Short k) {
			return get(k.shortValue());
		}
		return null;
	}

	@Override
	public V put(Short key, V value) {
		if (key == null) {
			throw new NullPointerException("ShortObjectHashMap does not permit null keys");
		}
		return put(key.shortValue(), value);
	}

	@Override
	public boolean containsKey(Object key) {
		if (key instanceof Short k) {
			return containsKey(k.shortValue());
		}
		return false;
	}

	@Override
	public V remove(Object key) {
		if (key instanceof Short k) {
			return remove(k.shortValue());
		}
		return null;
	}

	@Override
	public boolean containsValue(Object value) {
		if (value == null) {
			for (int i = 0; i < used.length; i++) {
				if (used[i] != 0 && values[i] == null) {
					return true;
				}
			}
		} else {
			for (int i = 0; i < used.length; i++) {
				if (used[i] != 0 && value.equals(values[i])) {
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
		Arrays.fill(values, null);
		size = 0;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void forEach(BiConsumer<? super Short, ? super V> action) {
		for (int i = 0; i < used.length; i++) {
			if (used[i] != 0) {
				action.accept(keys[i], (V) values[i]);
			}
		}
	}

	@Override
	public Set<Short> keySet() {
		return new AbstractSet<>() {
			@Override
			public Iterator<Short> iterator() {
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
					public Short next() {
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
				return ShortObjectHashMap.this.size();
			}

			@Override
			public boolean contains(Object o) {
				return ShortObjectHashMap.this.containsKey(o);
			}

			@Override
			public boolean remove(Object o) {
				return ShortObjectHashMap.this.remove(o) != null;
			}

			@Override
			public void clear() {
				ShortObjectHashMap.this.clear();
			}
		};
	}

	@Override
	public Collection<V> values() {
		return new AbstractCollection<>() {
			@Override
			public Iterator<V> iterator() {
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

					@SuppressWarnings("unchecked")
					@Override
					public V next() {
						if (!hasNext()) {
							throw new NoSuchElementException();
						}
						int idx = nextIdx;
						nextIdx = advance(nextIdx + 1);
						return (V) values[idx];
					}
				};
			}

			@Override
			public int size() {
				return ShortObjectHashMap.this.size();
			}

			@Override
			public boolean contains(Object o) {
				return ShortObjectHashMap.this.containsValue(o);
			}

			@Override
			public void clear() {
				ShortObjectHashMap.this.clear();
			}
		};
	}

	@Override
	public Set<Entry<Short, V>> entrySet() {
		return new AbstractSet<>() {
			@Override
			public Iterator<Entry<Short, V>> iterator() {
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

					@SuppressWarnings("unchecked")
					@Override
					public Entry<Short, V> next() {
						if (!hasNext()) {
							throw new NoSuchElementException();
						}
						int idx = nextIdx;
						nextIdx = advance(nextIdx + 1);
						return new AbstractMap.SimpleImmutableEntry<>(keys[idx], (V) values[idx]);
					}
				};
			}

			@Override
			public int size() {
				return ShortObjectHashMap.this.size();
			}

			@Override
			public void clear() {
				ShortObjectHashMap.this.clear();
			}
		};
	}

	@SuppressWarnings("unchecked")
	@Override
	public ShortObjectHashMap<V> clone() {
		try {
			ShortObjectHashMap<V> clone = (ShortObjectHashMap<V>) super.clone();
			clone.keys = keys.clone();
			clone.values = values.clone();
			clone.used = used.clone();
			return clone;
		} catch (CloneNotSupportedException e) {
			throw new AssertionError(e);
		}
	}

	// ==================== 内部开地址散列引擎 / Internal Open-Addressing Engine ====================

	private int indexOf(short key) {
		int index = spread(key) & mask;
		while (used[index] != 0 && keys[index] != key) {
			index = (index + 1) & mask;
		}
		return index;
	}

	private void insert(int index, short key, V value) {
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
				values[last] = null;
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
		short[] oldKeys = keys;
		Object[] oldValues = values;
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
		keys = new short[capacity];
		values = new Object[capacity];
		used = new byte[capacity];
		mask = capacity - 1;
		size = 0;
		resizeThreshold = Math.max(MIN_CAPACITY, (int) (capacity * LOAD_FACTOR));
	}

	private static int spread(short key) {
		int h = (key & 0xFFFF) * 0x9E3779B9;
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
