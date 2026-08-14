package com.aionemu.gameserver.utils.collections;

import java.io.Serializable;
import java.util.Map;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 最近使用（LRU 风格）有界缓存。
 * Last-used (LRU-style) bounded cache.
 * <p>
 * 访问或更新会将条目移到链表头部；超出容量时淘汰尾部最久未用项。
 * Access or update moves an entry to the list head; overflow evicts the least-recent tail.
 *
 * @param <K> 键类型，须可比较 / Key type, must be comparable
 * @param <V> 值类型 / Value type
 * @author Rolandas
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class LastUsedCache<K extends Comparable, V> implements ICache<K, V>, Serializable {

	/**
	 * 序列化版本。
	 * Serialization version.
	 */
	private static final long serialVersionUID = 3674312987828041877L;

	/**
	 * 键到链表节点的映射。
	 * Map from keys to list items.
	 */
	Map<K, Item> map = new ConcurrentHashMap<>();

	/**
	 * 链表哨兵头节点。
	 * Sentinel head of the doubly linked list.
	 */
	Item startItem = new Item();

	/**
	 * 链表哨兵尾节点。
	 * Sentinel tail of the doubly linked list.
	 */
	Item endItem = new Item();

	/**
	 * 最大容量；0 表示不限制。
	 * Max capacity; 0 means unlimited.
	 */
	int maxSize;

	/**
	 * 链表操作同步锁。
	 * Sync root for list mutations.
	 */
	private final Object syncRoot = new Object();

	/**
	 * 双向链表节点。
	 * Doubly linked list node.
	 */
	static class Item {

		/**
		 * 使用键值构造。
		 * Construct with key and value.
		 *
		 * @param k 键 / Key
		 * @param v 值 / Value
		 */
		public Item(Comparable k, Object v) {
			key = k;
			value = v;
		}

		/**
		 * 空节点构造（哨兵）。
		 * Empty node constructor (sentinels).
		 */
		public Item() {
		}

		/**
		 * 节点键。
		 * Node key.
		 */
		public Comparable key;

		/**
		 * 节点值。
		 * Node value.
		 */
		public Object value;

		/**
		 * 前驱节点。
		 * Previous node.
		 */
		public Item previous;

		/**
		 * 后继节点。
		 * Next node.
		 */
		public Item next;
	}

	/**
	 * 从双向链表中摘除节点。
	 * Unlink an item from the doubly linked list.
	 *
	 * @param item 待移除节点 / Item to remove
	 */
	void removeItem(Item item) {
		synchronized (syncRoot) {
			item.previous.next = item.next;
			item.next.previous = item.previous;
		}
	}

	/**
	 * 将节点插入到链表头部。
	 * Insert an item at the list head.
	 *
	 * @param item 待插入节点 / Item to insert
	 */
	void insertHead(Item item) {
		synchronized (syncRoot) {
			item.previous = startItem;
			item.next = startItem.next;
			startItem.next.previous = item;
			startItem.next = item;
		}
	}

	/**
	 * 将已有节点移动到链表头部。
	 * Move an existing item to the list head.
	 *
	 * @param item 待移动节点 / Item to move
	 */
	void moveToHead(Item item) {
		synchronized (syncRoot) {
			item.previous.next = item.next;
			item.next.previous = item.previous;
			item.previous = startItem;
			item.next = startItem.next;
			startItem.next.previous = item;
			startItem.next = item;
		}
	}

	/**
	 * 使用最大容量构造。
	 * Construct with a maximum object count.
	 *
	 * @param maxObjects 最大条目数，0 表示不限制 / Max entries, 0 means unlimited
	 */
	public LastUsedCache(int maxObjects) {
		maxSize = maxObjects;
		startItem.next = endItem;
		endItem.previous = startItem;
	}

	/**
	 * 返回全部缓存键值对（按最近使用顺序）。
	 * Return all cache pairs (most-recent first).
	 *
	 * @return 键值对数组 / Array of pairs
	 */
	@Override
	public CachePair[] getAll() {
		CachePair p[] = new CachePair[maxSize];
		int count = 0;

		synchronized (syncRoot) {
			Item cur = startItem.next;
			while (cur != endItem) {
				p[count] = new CachePair(cur.key, cur.value);
				count++;
				cur = cur.next;
			}
		}
		CachePair np[] = new CachePair[count];
		System.arraycopy(p, 0, np, 0, count);
		return np;
	}

	/**
	 * 按键获取值；命中时提升为最近使用。未找到返回 null。
	 * Get a value by key; promote to most-recent on hit. Returns null if not found.
	 *
	 * @param key 键 / Key
	 * @return 缓存值，不存在时为 null / Value or null
	 */
	@Override
	public V get(K key) {
		Item cur = map.get(key);
		if (cur == null) {
			return null;
		}
		if (cur != startItem.next) {
			moveToHead(cur);
		}
		return (V) cur.value;
	}

	/**
	 * 添加或更新缓存项；已存在则更新并提升；满则淘汰最久未用。
	 * Add or renew a cache pair; update and promote if present; evict least-recent when full.
	 *
	 * @param key 键 / Key
	 * @param value 值 / Value
	 */
	@Override
	public void put(K key, V value) {
		Item cur = map.get(key);
		if (cur != null) {
			cur.value = value;
			moveToHead(cur);
			return;
		}

		if (map.size() >= maxSize && maxSize != 0) {
			cur = endItem.previous;
			map.remove(cur.key);
			removeItem(cur);
		}
		Item item = new Item(key, value);
		insertHead(item);
		map.put(key, item);
	}

	/**
	 * 按键移除缓存项。
	 * Remove a cache entry by key.
	 *
	 * @param key 键 / Key
	 */
	@Override
	public void remove(K key) {
		Item cur = map.get(key);
		if (cur == null) {
			return;
		}
		map.remove(key);
		removeItem(cur);
	}

	/**
	 * 当前缓存大小。
	 * Current cache size.
	 *
	 * @return 条目数 / Entry count
	 */
	@Override
	public int size() {
		return map.size();
	}
}
