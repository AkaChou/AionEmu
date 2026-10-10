package com.aionemu.commons.utils.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class IntIntHashMapTest {

	@Test
	void storesAndReadsPrimitiveValuesWithoutBoxing() {
		IntIntHashMap map = new IntIntHashMap();

		assertEquals(0, map.put(101, 202));
		assertEquals(202, map.get(101));
		assertEquals(0, map.get(999));
		assertEquals(1, map.size());
		assertFalse(map.isEmpty());
		assertTrue(map.containsKey(101));
		assertTrue(map.contains(101));
		assertFalse(map.containsKey(999));
	}

	@Test
	void overwriteReturnsPreviousValue() {
		IntIntHashMap map = new IntIntHashMap();
		assertEquals(0, map.put(1, 10));
		assertEquals(10, map.put(1, 20));
		assertEquals(20, map.get(1));
		assertEquals(1, map.size());
	}

	@Test
	void removesKeyAndReturnsPreviousValue() {
		IntIntHashMap map = new IntIntHashMap();
		map.put(5, 50);
		map.put(6, 60);

		assertEquals(50, map.remove(5));
		assertEquals(0, map.get(5));
		assertFalse(map.containsKey(5));
		assertEquals(1, map.size());

		assertEquals(0, map.remove(99));
		assertEquals(1, map.size());
	}

	@Test
	void handlesCollisionsAndBackwardShiftDeletion() {
		IntIntHashMap map = new IntIntHashMap(8);

		for (int i = 0; i < 20; i++) {
			map.put(i, i * 10);
		}
		assertEquals(20, map.size());

		assertEquals(50, map.remove(5));
		assertEquals(100, map.remove(10));
		assertEquals(150, map.remove(15));
		assertEquals(17, map.size());

		assertEquals(0, map.get(5));
		assertEquals(0, map.get(10));
		assertEquals(0, map.get(15));

		for (int i = 0; i < 20; i++) {
			if (i != 5 && i != 10 && i != 15) {
				assertEquals(i * 10, map.get(i));
				assertTrue(map.containsKey(i));
			}
		}
	}

	@Test
	void growsAndPreservesEntries() {
		IntIntHashMap map = new IntIntHashMap(8);
		int count = 5_000;

		for (int i = 0; i < count; i++) {
			map.put(i, i + 100);
		}
		assertEquals(count, map.size());

		for (int i = 0; i < count; i++) {
			assertEquals(i + 100, map.get(i));
		}
	}

	@Test
	void keysAndValuesArrays() {
		IntIntHashMap map = new IntIntHashMap();
		map.put(3, 30);
		map.put(1, 10);
		map.put(2, 20);

		int[] keys = map.keys();
		int[] values = map.valuesArray();

		assertEquals(3, keys.length);
		assertEquals(3, values.length);

		Arrays.sort(keys);
		Arrays.sort(values);
		assertEquals(1, keys[0]);
		assertEquals(2, keys[1]);
		assertEquals(3, keys[2]);

		assertEquals(10, values[0]);
		assertEquals(20, values[1]);
		assertEquals(30, values[2]);
	}

	@Test
	void forEachEntryWithoutBoxing() {
		IntIntHashMap map = new IntIntHashMap();
		map.put(10, 100);
		map.put(20, 200);

		List<Integer> gatheredKeys = new ArrayList<>();
		List<Integer> gatheredVals = new ArrayList<>();

		map.forEachEntry((k, v) -> {
			gatheredKeys.add(k);
			gatheredVals.add(v);
		});

		assertEquals(2, gatheredKeys.size());
		assertTrue(gatheredKeys.contains(10));
		assertTrue(gatheredKeys.contains(20));
		assertTrue(gatheredVals.contains(100));
		assertTrue(gatheredVals.contains(200));
	}

	@Test
	void clearAndReuse() {
		IntIntHashMap map = new IntIntHashMap(16);
		for (int i = 0; i < 50; i++) {
			map.put(i, i);
		}
		assertEquals(50, map.size());

		map.clear();
		assertEquals(0, map.size());
		assertTrue(map.isEmpty());
		assertEquals(0, map.get(0));

		map.put(7, 77);
		assertEquals(1, map.size());
		assertEquals(77, map.get(7));
	}

	@Test
	void mapInterfaceCompatibility() {
		Map<Integer, Integer> map = new IntIntHashMap();
		assertNull(map.put(12, 120));
		assertEquals(120, map.get(12));
		assertNull(map.get("invalid_key"));
		assertTrue(map.containsKey(12));
		assertFalse(map.containsKey("invalid_key"));
		assertTrue(map.containsValue(120));
		assertFalse(map.containsValue(999));

		assertEquals(120, map.remove(12));
		assertNull(map.remove("invalid_key"));
	}

	@Test
	void cloneCreatesIndependentCopy() {
		IntIntHashMap original = new IntIntHashMap();
		original.put(1, 10);
		original.put(2, 20);

		IntIntHashMap clone = original.clone();
		assertNotSame(original, clone);
		assertEquals(2, clone.size());
		assertEquals(10, clone.get(1));
		assertEquals(20, clone.get(2));

		clone.put(3, 30);
		assertEquals(3, clone.size());
		assertEquals(2, original.size());
		assertEquals(0, original.get(3));
	}
}
