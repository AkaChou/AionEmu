package com.aionemu.gameserver.utils.collections;

/**
 * 可比较的缓存键值对。
 * Comparable cache key-value pair.
 *
 * @param <K> 键类型，须可比较 / Key type, must be comparable
 * @param <V> 值类型 / Value type
 * @author Rolandas
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class CachePair<K extends Comparable, V> implements Comparable<CachePair> {

	/**
	 * 使用键值构造。
	 * Construct with key and value.
	 *
	 * @param key 键 / Key
	 * @param value 值 / Value
	 */
	public CachePair(K key, V value) {
		this.key = key;
		this.value = value;
	}

	/**
	 * 缓存键。
	 * Cache key.
	 */
	public K key;

	/**
	 * 缓存值。
	 * Cache value.
	 */
	public V value;

	/**
	 * 按键值是否都相等判断相等。
	 * Equality by both key and value.
	 *
	 * @param obj 比较对象 / Object to compare
	 * @return 若 equal 则为 true / True if equal
	 */
	public boolean equals(Object obj) {
		if (obj instanceof CachePair) {
			CachePair p = (CachePair) obj;
			return key.equals(p.key) && value.equals(p.value);
		}
		return false;
	}

	/**
	 * 先比键，键相同且值可比较时再比值。
	 * Compare by key first; if equal and value is comparable, compare values.
	 *
	 * @param p 另一对 / Other pair
	 * @return 比较结果 / Comparison result
	 */
	public int compareTo(CachePair p) {
		int v = key.compareTo(p.key);
		if (v == 0 && p.value instanceof Comparable)
			return ((Comparable) value).compareTo(p.value);
		return v;
	}

	/**
	 * 基于键值的哈希码。
	 * Hash code based on key and value.
	 *
	 * @return 哈希码 / Hash code
	 */
	@Override
	public int hashCode() {
		int result = key.hashCode();
		result = 37 * result + value.hashCode();
		return result;
	}

	/**
	 * 格式化为 {@code key: value}。
	 * Format as {@code key: value}.
	 *
	 * @return 字符串表示 / String representation
	 */
	@Override
	public String toString() {
		return key + ": " + value;
	}
}
