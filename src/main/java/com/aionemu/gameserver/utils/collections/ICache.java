package com.aionemu.gameserver.utils.collections;

/**
 * 通用缓存接口。
 * Generic cache interface.
 *
 * @param <K> 键类型，须可比较 / Key type, must be comparable
 * @param <V> 值类型 / Value type
 * @author Rolandas
 */
@SuppressWarnings({ "rawtypes" })
public interface ICache<K extends Comparable, V> {

	/**
	 * 按键获取缓存值。
	 * Get a cached value by key.
	 *
	 * Key
	 * @return 缓存值，不存在则为 null / Cached value, or null if absent
	 */
	V get(K obj);

	/**
	 * 写入或更新缓存项。
	 * Put or renew a cache entry.
	 *
	 * Key
	 * Value
	 */
	void put(K key, V obj);

	/**
	 * 按键移除缓存项。
	 * Remove a cache entry by key.
	 *
	 * Key
	 */
	void remove(K key);

	/**
	 * 返回全部缓存键值对。
	 * Return all cache key-value pairs.
	 *
	 * @return 键值对数组 / Array of pairs
	 */
	CachePair[] getAll();

	/**
	 * 当前缓存大小。
	 * Current cache size.
	 *
	 * Entry count
	 */
	int size();
}
