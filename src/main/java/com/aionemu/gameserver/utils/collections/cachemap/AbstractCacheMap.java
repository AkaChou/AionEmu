package com.aionemu.gameserver.utils.collections.cachemap;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.util.HashMap;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

/**
 * {@link WeakCacheMap} 与 {@link SoftCacheMap} 的基类。
 * Base class for {@link WeakCacheMap} and {@link SoftCacheMap}.
 *
 * @param <K> 键类型 / Key type
 * @param <V> 值类型 / Value type
 * @author Luno
 */
@Slf4j
abstract class AbstractCacheMap<K, V> implements CacheMap<K, V> {

	/**
	 * 缓存显示名称（带前缀）。
	 * Display cache name with prefix.
	 */
	protected final String cacheName;

	/**
	 * 值的助记名称。
	 * Mnemonic name for cached values.
	 */
	protected final String valueName;

	/**
	 * 存储缓存对象引用的映射。
	 * Map storing references to cached objects.
	 */
	protected final Map<K, Reference<V>> cacheMap = new HashMap<K, Reference<V>>();

	/**
	 * 引用回收队列。
	 * Reference queue for cleared entries.
	 */
	protected final ReferenceQueue<V> refQueue = new ReferenceQueue<V>();

	/**
	 * 使用缓存名与值名构造。
	 * Construct with cache name and value name.
	 *
	 * Cache name
	 * Value name
	 */
	AbstractCacheMap(String cacheName, String valueName) {
		this.cacheName = "#CACHE  [" + cacheName + "]#  ";
		this.valueName = valueName;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void put(K key, V value) {
		cleanQueue();

		if (cacheMap.containsKey(key)) {
			throw new IllegalArgumentException("Key: " + key + " already exists in map");
		}
		Reference<V> entry = newReference(key, value, refQueue);

		cacheMap.put(key, entry);

		if (log.isDebugEnabled()) {
			log.debug("{} : added {} for key: {}", cacheName, valueName, key);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public V get(K key) {
		cleanQueue();

		Reference<V> reference = cacheMap.get(key);

		if (reference == null) {
			return null;
		}
		V res = reference.get();

		if (res != null && log.isDebugEnabled()) {
			log.debug("{} : obtained {} for key: {}", cacheName, valueName, key);
		}
		return res;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean contains(K key) {
		cleanQueue();
		return cacheMap.containsKey(key);
	}

	/**
	 * 清理引用队列中已失效的条目。
	 * Clean up entries cleared from the reference queue.
	 */
	protected abstract void cleanQueue();

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void remove(K key) {
		cacheMap.remove(key);
	}

	/**
	 * 创建包装键的引用实现。
	 * Create a reference implementation that holds the key.
	 *
	 * Key
	 * Value
	 * @param queue 引用队列 / Reference queue
	 * Reference object
	 */
	protected abstract Reference<V> newReference(K key, V value, ReferenceQueue<V> queue);
}
