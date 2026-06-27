package com.aionemu.commons.utils.collections;

import java.util.LinkedHashMap;

public class ShortObjectHashMap<V> extends LinkedHashMap<Short, V> {

	public boolean contains(short key) {
		return containsKey(key);
	}

	public short[] keys() {
		short[] keys = new short[size()];
		int index = 0;
		for (short key : keySet()) {
			keys[index++] = key;
		}
		return keys;
	}
}
