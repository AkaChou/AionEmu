package com.aionemu.gameserver.utils.collections.cachemap;

import com.aionemu.gameserver.configs.main.CacheConfig;

/**
 * 缓存映射工厂，按配置创建弱引用或软引用实现。
 * Cache map factory that creates weak or soft implementations by config.
 *
 * @author Luno
 */
public class CacheMapFactory {

	/**
	 * 按 {@link CacheConfig#SOFT_CACHE_MAP} 返回 {@link SoftCacheMap} 或 {@link WeakCacheMap}。
	 * Return either {@link SoftCacheMap} or {@link WeakCacheMap} based on {@link CacheConfig#SOFT_CACHE_MAP}.
	 *
	 * @param <K> 键类型 / Type of keys
	 * @param <V> 值类型 / Type of values
	 * The name for this cache map
	 *
	 * @param valueName 值的助记名 / Mnemonic name for values stored in the cache
	 * @param valueName @return 缓存映射实例 / Cache map instance
	 */
	public static <K, V> CacheMap<K, V> createCacheMap(String cacheName, String valueName) {
		if (CacheConfig.SOFT_CACHE_MAP) {
			return createSoftCacheMap(cacheName, valueName);
		} else {
			return createWeakCacheMap(cacheName, valueName);
		}
	}

	/**
	 * 创建 {@link SoftCacheMap} 实例。
	 * Create and return a {@link SoftCacheMap} instance.
	 *
	 * @param <K> 键类型 / Type of keys
	 * @param <V> 值类型 / Type of values
	 * The name for this cache map
	 *
	 * @param valueName 值的助记名 / Mnemonic name for values stored in the cache
	 * @param valueName @return 软引用缓存映射 / Soft-reference cache map
	 */
	public static <K, V> CacheMap<K, V> createSoftCacheMap(String cacheName, String valueName) {
		return new SoftCacheMap<K, V>(cacheName, valueName);
	}

	/**
	 * 创建 {@link WeakCacheMap} 实例。
	 * Create and return a {@link WeakCacheMap} instance.
	 *
	 * @param <K> 键类型 / Type of keys
	 * @param <V> 值类型 / Type of values
	 * The name for this cache map
	 *
	 * @param valueName 值的助记名 / Mnemonic name for values stored in the cache
	 * @param valueName @return 弱引用缓存映射 / Weak-reference cache map
	 */
	public static <K, V> CacheMap<K, V> createWeakCacheMap(String cacheName, String valueName) {
		return new WeakCacheMap<K, V>(cacheName, valueName);
	}
}
