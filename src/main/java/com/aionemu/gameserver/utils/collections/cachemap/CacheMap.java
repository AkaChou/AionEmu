package com.aionemu.gameserver.utils.collections.cachemap;

/**
 * 供缓存使用的 Map 结构接口。
 * Map structure interface for cache usage.
 *
 * @param <K> 键类型 / Key type
 * @param <V> 值类型 / Value type
 * @author Luno
 */
public interface CacheMap<K, V> {

	/**
	 * 向缓存映射添加一对键值。
	 * Add a key-value pair to the cache map.
	 * <p>
	 * 若给定键已存在，将抛出 {@link IllegalArgumentException}。
	 * Throws {@link IllegalArgumentException} if the key already exists.
	 *
	 * @param key 键 / Key
	 * @param value 值 / Value
	 */
	public void put(K key, V value);

	/**
	 * 返回与给定键关联的缓存值。
	 * Return the cached value correlated to the given key.
	 *
	 * @param key 键 / Key
	 * @return 缓存值，不存在则为 null / Cached value, or null if absent
	 */
	public V get(K key);

	/**
	 * 是否包含与给定键相关的值。
	 * Whether this map contains a value for the given key.
	 *
	 * @param key 键 / Key
	 * 存在则为 true / True if present
	 */
	public boolean contains(K key);

	/**
	 * 移除给定键对应的条目。
	 * Remove the entry with the given key.
	 *
	 * @param key 键 / Key
	 */
	public void remove(K key);
}
