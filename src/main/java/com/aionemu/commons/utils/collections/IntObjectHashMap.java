package com.aionemu.commons.utils.collections;

import java.util.LinkedHashMap;

public class IntObjectHashMap<V> extends LinkedHashMap<Integer, V> {

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
