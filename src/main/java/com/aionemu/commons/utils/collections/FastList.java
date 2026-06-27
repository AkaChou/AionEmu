package com.aionemu.commons.utils.collections;

import java.util.ArrayList;
import java.util.Collection;

public class FastList<E> extends ArrayList<E> {

	public FastList() {
	}

	public FastList(int initialCapacity) {
		super(initialCapacity);
	}

	public FastList(Collection<? extends E> values) {
		super(values);
	}

	public static <E> FastList<E> newInstance() {
		return new FastList<>();
	}

	public static void recycle(FastList<?> list) {
		if (list != null) {
			list.clear();
		}
	}

	public FastList<E> shared() {
		return this;
	}
}
