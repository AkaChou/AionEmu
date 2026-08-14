package com.aionemu.gameserver.utils.collections.cachemap;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

import lombok.extern.slf4j.Slf4j;

/**
 * 基于弱引用的简单缓存映射。
 * Simple cache map backed by weak references.
 * <p>
 * 若值对象无强引用，首次 GC 后条目会被移除。
 * Entries are removed after the first GC run when no strong reference to the value remains.
 *
 * @param <K> 键类型 / Key type
 * @param <V> 值类型 / Value type
 * @author Luno
 */
@Slf4j
class WeakCacheMap<K, V> extends AbstractCacheMap<K, V> implements CacheMap<K, V> {

	/**
	 * 带键信息的 {@link WeakReference}。
	 * {@link WeakReference} that also holds the key.
	 *
	 * @author Luno
	 */
	private class Entry extends WeakReference<V> {

		/**
		 * 关联键。
		 * Associated key.
		 */
		private K key;

		/**
		 * 使用键、引用对象与队列构造。
		 * Construct with key, referent and queue.
		 *
	 * @param key 键 / Key
	 * @param referent 引用对象 / Referent
		 * @param q 引用队列 / Reference queue
		 */
		Entry(K key, V referent, ReferenceQueue<? super V> q) {
			super(referent, q);
			this.key = key;
		}

		/**
		 * 返回关联键。
		 * Return the associated key.
		 *
		 * @return 键 / Key
		 */
		K getKey() {
			return key;
		}
	}

	/**
	 * 使用缓存名与值名构造。
	 * Construct with cache name and value name.
	 *
	 * @param cacheName 缓存名称 / Cache name
	 * @param valueName 值名称 / Value name
	 */
	WeakCacheMap(String cacheName, String valueName) {
		super(cacheName, valueName);
	}

	/**
	 * 从引用队列清理已被 GC 回收的弱引用条目。
	 * Clean weak-reference entries reclaimed by the GC from the queue.
	 */
	@Override
	@SuppressWarnings("unchecked")
	protected synchronized void cleanQueue() {
		Entry en = null;
		while ((en = (Entry) refQueue.poll()) != null) {
			K key = en.getKey();
			if (log.isDebugEnabled()) {
				log.debug("{} : cleaned up {} for key: {}", cacheName, valueName, key);
			}
			cacheMap.remove(key);
		}
	}

	/**
	 * 创建带键的弱引用。
	 * Create a weak reference holding the key.
	 *
	 * @param key 键 / Key
	 * @param value 值 / Value
	 * @param vReferenceQueue 引用队列 / Reference queue
	 * @return 弱引用 / Weak reference
	 */
	@Override
	protected Reference<V> newReference(K key, V value, ReferenceQueue<V> vReferenceQueue) {
		return new Entry(key, value, vReferenceQueue);
	}
}
