package com.aionemu.commons.utils.collections;

import java.util.ArrayList;
import java.util.Collection;

public class IntArrayList extends ArrayList<Integer> {

	public IntArrayList() {
	}

	public IntArrayList(Collection<Integer> values) {
		super(values);
	}

	public boolean add(int value) {
		return super.add(value);
	}

	public boolean contains(int value) {
		return super.contains(value);
	}

	public boolean forEach(IntProcedure procedure) {
		for (int value : this) {
			if (!procedure.execute(value)) {
				return false;
			}
		}
		return true;
	}
}
