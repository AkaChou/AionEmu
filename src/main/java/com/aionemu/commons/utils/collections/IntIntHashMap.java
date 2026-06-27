package com.aionemu.commons.utils.collections;

public class IntIntHashMap extends FastMap<Integer, Integer> {

	public int get(int key) {
		Integer value = super.get(key);
		return value == null ? 0 : value;
	}

	public int put(int key, int value) {
		Integer previous = super.put(key, value);
		return previous == null ? 0 : previous;
	}

	public boolean contains(int key) {
		return containsKey(key);
	}
}
