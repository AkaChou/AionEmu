package com.aionemu.gameserver.geoEngine.utils;

import java.util.Iterator;

import com.aionemu.gameserver.geoEngine.utils.IntMap.Entry;

/**
 * 以 int 为键的开放链式哈希表。
 * Open-chaining hash map with int keys.
 * <p>
 * 源自 http://code.google.com/p/skorpios/
 * Taken from http://code.google.com/p/skorpios/
 *
 * @param <T> 值类型 / value type
 * @author Nate
 */
@SuppressWarnings("rawtypes")
public final class IntMap<T> implements Iterable<Entry>, Cloneable {

	/** 桶数组。 / Bucket table. */
	private Entry[] table;
	/** 负载因子。 / Load factor. */
	private final float loadFactor;
	/** 当前条目数、掩码、容量与扩容阈值。 / Size, mask, capacity and resize threshold. */
	private int size, mask, capacity, threshold;

	/**
	 * 默认容量 16、负载因子 0.75。
	 * Default capacity 16 and load factor 0.75.
	 */
	public IntMap() {
		this(16, 0.75f);
	}

	/**
	 * 指定初始容量，负载因子 0.75。
	 * Constructs with given initial capacity and load factor 0.75.
	 *
	 * @param initialCapacity 初始容量 / initial capacity
	 */
	public IntMap(int initialCapacity) {
		this(initialCapacity, 0.75f);
	}

	/**
	 * 指定初始容量与负载因子（容量上取 2 的幂）。
	 * Constructs with initial capacity (rounded up to power of two) and load factor.
	 *
	 * @param initialCapacity 初始容量 / initial capacity
	 * @param loadFactor 负载因子 / load factor
	 */
	public IntMap(int initialCapacity, float loadFactor) {
		if (initialCapacity > 1 << 30) {
			throw new IllegalArgumentException("initialCapacity is too large.");
		}
		if (initialCapacity < 0) {
			throw new IllegalArgumentException("initialCapacity must be greater than zero.");
		}
		if (loadFactor <= 0) {
			throw new IllegalArgumentException("initialCapacity must be greater than zero.");
		}
		capacity = 1;
		while (capacity < initialCapacity) {
			capacity <<= 1;
		}
		this.loadFactor = loadFactor;
		this.threshold = (int) (capacity * loadFactor);
		this.table = new Entry[capacity];
		this.mask = capacity - 1;
	}

	/**
	 * 深克隆表项链表。
	 * Deep-clones the entry chains.
	 *
	 * @return 克隆后的映射 / the cloned map
	 */
	@Override
	@SuppressWarnings("unchecked")
	public IntMap<T> clone() {
		try {
			IntMap<T> clone = (IntMap<T>) super.clone();
			Entry[] newTable = new Entry[table.length];
			for (int i = table.length - 1; i >= 0; i--) {
				if (table[i] != null) {
					newTable[i] = table[i].clone();
				}
			}
			clone.table = newTable;
			return clone;
		} catch (CloneNotSupportedException ex) {
		}
		return null;
	}

	/**
	 * 是否包含给定值（equals 比较）。
	 * Whether any entry has the given value (equals comparison).
	 *
	 * @param value 查找的值 / the value to look up
	 * @return 存在则为 true / true if present
	 */
	public boolean containsValue(Object value) {
		Entry[] table = this.table;
		for (int i = table.length; i-- > 0;) {
			for (Entry e = table[i]; e != null; e = e.next) {
				if (e.value.equals(value)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * 是否包含给定键。
	 * Whether the map contains the given key.
	 *
	 * @param key 要查找的键 / the key to look up
	 * @return 存在则为 true / true if present
	 */
	public boolean containsKey(int key) {
		int index = (key) & mask;
		for (Entry e = table[index]; e != null; e = e.next) {
			if (e.key == key) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 按键取值，不存在返回 null。
	 * Returns the value for the key, or null if absent.
	 *
	 * @param key 键 / the key
	 * @return 值或 null / value or null
	 */
	@SuppressWarnings("unchecked")
	public T get(int key) {
		int index = key & mask;
		for (Entry e = table[index]; e != null; e = e.next) {
			if (e.key == key) {
				return (T) e.value;
			}
		}
		return null;
	}

	/**
	 * 放入键值对；键已存在则覆盖并返回旧值。超阈值时扩容重哈希。
	 * Puts the entry; overwrites and returns the old value if the key exists. Rehashes when threshold is exceeded.
	 *
	 * @param key 键 / the key
	 * @param value 值 / the value
	 * @return 旧值或 null / previous value or null
	 */
	@SuppressWarnings("unchecked")
	public T put(int key, T value) {
		int index = key & mask;
		// 检查键是否已存在。 / Check if key already exists.
		for (Entry e = table[index]; e != null; e = e.next) {
			if (e.key != key) {
				continue;
			}
			Object oldValue = e.value;
			e.value = value;
			return (T) oldValue;
		}
		table[index] = new Entry(key, value, table[index]);
		if (size++ >= threshold) {
			// 重哈希。 / Rehash.
			int newCapacity = 2 * capacity;
			Entry[] newTable = new Entry[newCapacity];
			Entry[] src = table;
			int bucketmask = newCapacity - 1;
			for (int j = 0; j < src.length; j++) {
				Entry e = src[j];
				if (e != null) {
					src[j] = null;
					do {
						Entry next = e.next;
						index = e.key & bucketmask;
						e.next = newTable[index];
						newTable[index] = e;
						e = next;
					} while (e != null);
				}
			}
			table = newTable;
			capacity = newCapacity;
			threshold = (int) (newCapacity * loadFactor);
			mask = capacity - 1;
		}
		return null;
	}

	/**
	 * 移除键并返回旧值。
	 * Removes the key and returns the previous value.
	 *
	 * @param key 要移除的键 / the key to remove
	 * @return 旧值或 null / previous value or null
	 */
	@SuppressWarnings("unchecked")
	public T remove(int key) {
		int index = key & mask;
		Entry prev = table[index];
		Entry e = prev;
		while (e != null) {
			Entry next = e.next;
			if (e.key == key) {
				size--;
				if (prev == e) {
					table[index] = next;
				} else {
					prev.next = next;
				}
				return (T) e.value;
			}
			prev = e;
			e = next;
		}
		return null;
	}

	/**
	 * 当前条目数。
	 * Number of entries.
	 *
	 * @return 当前条目数 / the current size
	 */
	public int size() {
		return size;
	}

	/**
	 * 清空所有条目。
	 * Clears all entries.
	 */
	public void clear() {
		Entry[] table = this.table;
		for (int index = table.length; --index >= 0;) {
			table[index] = null;
		}
		size = 0;
	}

	/**
	 * 返回遍历所有条目的迭代器。
	 * Returns an iterator over all entries.
	 *
	 * @return 条目迭代器 / the entry iterator
	 */
	@Override
	public Iterator<Entry> iterator() {
		return new IntMapIterator();
	}

	/**
	 * 顺序遍历桶与链表的条目迭代器。
	 * Entry iterator walking buckets and chains in order.
	 */
	final class IntMapIterator implements Iterator<Entry> {

		/** 当前链表节点。 / Current chain entry. */
		private Entry cur;
		/** 当前桶下标。 / Current bucket index. */
		private int idx = 0;
		/** 已返回元素计数。 / Count of elements yielded. */
		private int el = 0;

		/**
		 * 从第一个桶开始。
		 * Starts at the first bucket.
		 */
		public IntMapIterator() {
			cur = table[0];
		}

		/**
		 * 是否还有元素。
		 * Whether more elements remain.
		 *
		 * @return 有下一元素 / true if more
		 */
		@Override
		public boolean hasNext() {
			return el < size;
		}

		/**
		 * 返回下一个条目。
		 * Returns the next entry.
		 *
		 * @return 下一个条目 / the next entry
		 */
		@Override
		public Entry next() {
			if (el >= size) {
				throw new IllegalStateException("No more elements!");
			}

			if (cur != null) {
				Entry e = cur;
				cur = cur.next;
				el++;
				return e;
			}
			// if (cur != null && cur.next != null){
			// 若有当前条目，继续列表中下一项 / if we have a current entry, continue to the next entry in the list
			// cur = cur.next;
			// el++;
			// return cur;
			// }

			do {
				// 要么当前条目列表已耗尽，要么 / either we exhausted the current entry list, or
				// 条目为 null。查找另一个非 null 条目。 / the entry was null. find another non-null entry.
				cur = table[++idx];
			} while (cur == null);
			Entry e = cur;
			cur = cur.next;
			el++;
			return e;
		}

		/**
		 * 不支持删除。
		 * Remove is unsupported (no-op).
		 */
		@Override
		public void remove() {
		}
	}

	/**
	 * 哈希表条目（键、值与同桶下一节点）。
	 * Hash-map entry holding key, value and next link in the bucket.
	 *
	 * @param <T> 值类型 / value type
	 */
	public static final class Entry<T> implements Cloneable {

		/** 键。 / Key. */
		final int key;
		/** 值。 / Value. */
		T value;
		/** 同桶下一节点。 / Next entry in the chain. */
		Entry next;

		/**
		 * 构造条目。
		 * Constructs an entry.
		 *
		 * @param k 键 / key
		 * @param v 值 / value
		 * @param n 下一节点 / next entry
		 */
		Entry(int k, T v, Entry n) {
			key = k;
			value = v;
			next = n;
		}

		/**
		 * 返回键。
		 * Returns the key.
		 *
		 * @return 键 / the key
		 */
		public int getKey() {
			return key;
		}

		/**
		 * 返回值。
		 * Returns the value.
		 *
		 * @return 值 / the value
		 */
		public T getValue() {
			return value;
		}

		/**
		 * 键值字符串表示。
		 * String form "key => value".
		 *
		 * @return 字符串表示 / the string form
		 */
		@Override
		public String toString() {
			return key + " => " + value;
		}

		/**
		 * 深克隆链表。
		 * Deep-clones the chain starting at this entry.
		 *
		 * @return 克隆的条目 / the cloned entry
		 */
		@Override
		@SuppressWarnings("unchecked")
		public Entry<T> clone() {
			try {
				Entry<T> clone = (Entry<T>) super.clone();
				clone.next = next != null ? next.clone() : null;
				return clone;
			} catch (CloneNotSupportedException ex) {
			}
			return null;
		}
	}
}
