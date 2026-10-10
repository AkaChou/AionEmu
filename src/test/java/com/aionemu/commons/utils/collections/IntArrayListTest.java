package com.aionemu.commons.utils.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class IntArrayListTest {

	@Test
	void addsAndRetrievesPrimitivesWithoutBoxing() {
		IntArrayList list = new IntArrayList();
		assertTrue(list.isEmpty());
		assertEquals(0, list.size());

		list.add(10);
		list.add(20);
		list.add(30);

		assertEquals(3, list.size());
		assertFalse(list.isEmpty());
		assertEquals(10, list.getInt(0));
		assertEquals(20, list.getInt(1));
		assertEquals(30, list.getInt(2));

		assertEquals(20, list.setInt(1, 25));
		assertEquals(25, list.getInt(1));
	}

	@Test
	void containsPrimitiveCheck() {
		IntArrayList list = new IntArrayList();
		list.add(100);
		list.add(200);

		assertTrue(list.contains(100));
		assertTrue(list.contains(200));
		assertFalse(list.contains(300));

		Object obj100 = 100;
		Object objOther = "string";
		assertTrue(list.contains(obj100));
		assertFalse(list.contains(objOther));
	}

	@Test
	void dynamicGrowthBeyondDefaultCapacity() {
		IntArrayList list = new IntArrayList(4);
		for (int i = 0; i < 1_000; i++) {
			list.add(i);
		}

		assertEquals(1_000, list.size());
		for (int i = 0; i < 1_000; i++) {
			assertEquals(i, list.getInt(i));
		}
	}

	@Test
	void removeAtRemovesCorrectElementAndShifts() {
		IntArrayList list = new IntArrayList();
		list.add(1);
		list.add(2);
		list.add(3);
		list.add(4);

		assertEquals(2, list.removeAt(1));
		assertEquals(3, list.size());
		assertEquals(1, list.getInt(0));
		assertEquals(3, list.getInt(1));
		assertEquals(4, list.getInt(2));

		assertEquals(4, list.remove(2));
		assertEquals(2, list.size());

		assertThrows(IndexOutOfBoundsException.class, () -> list.getInt(2));
	}

	@Test
	void forEachProcedureWithEarlyTermination() {
		IntArrayList list = new IntArrayList();
		list.add(1);
		list.add(2);
		list.add(3);
		list.add(4);

		List<Integer> visited = new ArrayList<>();
		boolean completed = list.forEach(val -> {
			visited.add(val);
			return val < 3;
		});

		assertFalse(completed);
		assertEquals(List.of(1, 2, 3), visited);
	}

	@Test
	void toIntArrayReturnsSnapshot() {
		IntArrayList list = new IntArrayList();
		list.add(7);
		list.add(8);

		int[] array = list.toIntArray();
		assertEquals(2, array.length);
		assertEquals(7, array[0]);
		assertEquals(8, array[1]);

		list.add(9);
		assertEquals(2, array.length);
	}

	@Test
	void listInterfaceCompatibilityAndStream() {
		List<Integer> list = new IntArrayList();
		list.add(10);
		list.add(20);
		list.add(30);

		assertEquals(3, list.size());
		assertEquals(20, list.get(1));

		int sum = list.stream().mapToInt(Integer::intValue).sum();
		assertEquals(60, sum);
	}

	@Test
	void cloneCreatesIndependentCopy() {
		IntArrayList original = new IntArrayList();
		original.add(100);
		original.add(200);

		IntArrayList clone = original.clone();
		assertNotSame(original, clone);
		assertEquals(2, clone.size());
		assertEquals(100, clone.getInt(0));

		clone.add(300);
		assertEquals(3, clone.size());
		assertEquals(2, original.size());
	}

	@Test
	void trimToSizeAndClear() {
		IntArrayList list = new IntArrayList(50);
		list.add(1);
		list.add(2);
		list.trimToSize();
		assertEquals(2, list.size());

		list.clear();
		assertEquals(0, list.size());
		assertTrue(list.isEmpty());
	}
}
