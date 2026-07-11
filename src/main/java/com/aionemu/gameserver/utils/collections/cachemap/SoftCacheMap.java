package com.aionemu.gameserver.utils.collections.cachemap;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;

import lombok.extern.slf4j.Slf4j;

/**
 * 基于软引用的简单缓存映射。
 * Simple cache map backed by soft references.
 * <p>
 * 值可长期驻留，但在内存紧张且无强引用时一定会被回收。
 * Values may stay for a long time, but will be reclaimed under low memory
 * when no strong references remain.
 *
 * @param <K> 键类型 / Key type
 * @param <V> 值类型 / Value type
 * @author Luno
 */
@Slf4j
class SoftCacheMap<K, V> extends AbstractCacheMap<K, V> implements CacheMap<K, V> {

	/**
	 * 带键信息的 {@link SoftReference}。
	 * {@link SoftReference} that also holds the key.
	 *
	 * @author Luno
	 */
	private class SoftEntry extends SoftReference<V> {

		/**
		 * 关联键。
		 * Associated key.
		 */
		private K key;

		/**
		 * 使用键、引用对象与队列构造。
		 * Construct with key, referent and queue.
		 *
		 * Key
		 * Referent
		 * @param q 引用队列 / Reference queue
		 */
		SoftEntry(K key, V referent, ReferenceQueue<? super V> q) {
			super(referent, q);
			this.key = key;
		}

		/**
		 * 返回关联键。
		 * Return the associated key.
		 *
		 * Key
		 */
		K getKey() {
			return key;
		}
	}

	/**
	 * 使用缓存名与值名构造。
	 * Construct with cache name and value name.
	 *
	 * Cache name
	 * Value name
	 */
	SoftCacheMap(String cacheName, String valueName) {
		super(cacheName, valueName);
	}

	/**
	 * 从引用队列清理已被 GC 回收的软引用条目。
	 * Clean soft-reference entries reclaimed by the GC from the queue.
	 */
	@Override
	@SuppressWarnings("unchecked")
	protected synchronized void cleanQueue() {
		SoftEntry en = null;
		while ((en = (SoftEntry) refQueue.poll()) != null) {
			K key = en.getKey();
			if (log.isDebugEnabled()) {
				log.debug("{} : cleaned up {} for key: {}", cacheName, valueName, key);
			}
			cacheMap.remove(key);
		}
	}

	/**
	 * 创建带键的软引用。
	 * Create a soft reference holding the key.
	 *
	 * Key
	 * Value
	 * Reference queue
	 * Soft reference
	 */
	@Override
	protected Reference<V> newReference(K key, V value, ReferenceQueue<V> vReferenceQueue) {
		return new SoftEntry(key, value, vReferenceQueue);
	}
}
