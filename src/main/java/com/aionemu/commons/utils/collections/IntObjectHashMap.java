package com.aionemu.commons.utils.collections;

public class IntObjectHashMap<V> extends FastMap<Integer, V> {

	public IntObjectHashMap() {
	}

	public IntObjectHashMap(int initialCapacity) {
		super(initialCapacity);
	}

	public boolean contains(int key) {
		return containsKey(key);
	}

	public int[] keys() {
		return keySet().stream().mapToInt(Integer::intValue).toArray();
	}
}
