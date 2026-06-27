package com.aionemu.commons.utils.collections;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class FastMap<K, V> extends LinkedHashMap<K, V> {
	private transient Entry<K, V> tail;

	public FastMap() {
	}

	public FastMap(int initialCapacity) {
		super(initialCapacity);
	}

	public FastMap(Map<? extends K, ? extends V> source) {
		super(source);
	}

	public static <K, V> FastMap<K, V> newInstance() {
		return new FastMap<>();
	}

	public static void recycle(FastMap<?, ?> map) {
		if (map != null) {
			map.clear();
		}
	}

	public FastMap<K, V> shared() {
		return this;
	}

	public Collection<V> valueCollection() {
		return values();
	}

	public V putEntry(K key, V value) {
		return put(key, value);
	}

	public Entry<K, V> head() {
		Entry<K, V> head = new Entry<>(null, null);
		Entry<K, V> current = head;
		for (Map.Entry<K, V> entry : entrySet()) {
			Entry<K, V> next = new Entry<>(entry.getKey(), entry.getValue());
			current.next = next;
			current = next;
		}
		tail = new Entry<>(null, null);
		current.next = tail;
		return head;
	}

	public Entry<K, V> tail() {
		if (tail == null) {
			tail = new Entry<>(null, null);
		}
		return tail;
	}

	public static final class Entry<K, V> {
		private final K key;
		private final V value;
		private Entry<K, V> next;

		private Entry(K key, V value) {
			this.key = key;
			this.value = value;
		}

		public K getKey() {
			return key;
		}

		public V getValue() {
			return value;
		}

		public Entry<K, V> getNext() {
			return next;
		}
	}
}
