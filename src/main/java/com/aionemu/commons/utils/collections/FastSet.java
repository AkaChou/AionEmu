package com.aionemu.commons.utils.collections;

import java.util.Collection;
import java.util.HashSet;

public class FastSet<E> extends HashSet<E> {

	public FastSet() {
	}

	public FastSet(int initialCapacity) {
		super(initialCapacity);
	}

	public FastSet(Collection<? extends E> values) {
		super(values);
	}

	public static <E> FastSet<E> newInstance() {
		return new FastSet<>();
	}

	public FastSet<E> shared() {
		return this;
	}
}
