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

class ShortObjectHashMapTest {

	@Test
	void storesAndReadsValuesWithoutBoxing() {
		ShortObjectHashMap<String> map = new ShortObjectHashMap<>();

		assertNull(map.put((short) 10, "path_10"));
		assertEquals("path_10", map.get((short) 10));
		assertNull(map.get((short) 99));
		assertEquals(1, map.size());
		assertFalse(map.isEmpty());
		assertTrue(map.containsKey((short) 10));
		assertTrue(map.contains((short) 10));
		assertFalse(map.containsKey((short) 99));
	}

	@Test
	void overwriteReturnsPreviousValue() {
		ShortObjectHashMap<String> map = new ShortObjectHashMap<>();
		map.put((short) 1, "first");

		assertEquals("first", map.put((short) 1, "second"));
		assertEquals("second", map.get((short) 1));
		assertEquals(1, map.size());
	}

	@Test
	void removesKeyCorrectly() {
		ShortObjectHashMap<String> map = new ShortObjectHashMap<>();
		map.put((short) 5, "five");
		map.put((short) 6, "six");

		assertEquals("five", map.remove((short) 5));
		assertNull(map.get((short) 5));
		assertEquals(1, map.size());

		assertNull(map.remove((short) 99));
		assertEquals(1, map.size());
	}

	@Test
	void handlesCollisionsAndBackwardShiftDeletion() {
		ShortObjectHashMap<String> map = new ShortObjectHashMap<>(8);

		for (short i = 0; i < 20; i++) {
			map.put(i, "val_" + i);
		}
		assertEquals(20, map.size());

		assertEquals("val_5", map.remove((short) 5));
		assertEquals("val_10", map.remove((short) 10));
		assertEquals(18, map.size());

		assertNull(map.get((short) 5));
		assertNull(map.get((short) 10));

		for (short i = 0; i < 20; i++) {
			if (i != 5 && i != 10) {
				assertEquals("val_" + i, map.get(i));
			}
		}
	}

	@Test
	void growsAndPreservesEntries() {
		ShortObjectHashMap<String> map = new ShortObjectHashMap<>(8);
		int count = 2_000;

		for (short i = 0; i < count; i++) {
			map.put(i, "node_" + i);
		}
		assertEquals(count, map.size());

		for (short i = 0; i < count; i++) {
			assertEquals("node_" + i, map.get(i));
		}
	}

	@Test
	void keysArrayContainsAllKeys() {
		ShortObjectHashMap<String> map = new ShortObjectHashMap<>();
		map.put((short) 100, "a");
		map.put((short) 200, "b");

		short[] keys = map.keys();
		assertEquals(2, keys.length);
		Arrays.sort(keys);
		assertEquals((short) 100, keys[0]);
		assertEquals((short) 200, keys[1]);
	}

	@Test
	void valuesCollectionIteration() {
		ShortObjectHashMap<String> map = new ShortObjectHashMap<>();
		map.put((short) 1, "one");
		map.put((short) 2, "two");

		List<String> values = new ArrayList<>(map.values());
		assertEquals(2, values.size());
		assertTrue(values.contains("one"));
		assertTrue(values.contains("two"));
	}

	@Test
	void mapInterfaceCompatibility() {
		Map<Short, String> map = new ShortObjectHashMap<>();
		map.put((short) 12, "val_12");

		Object invalidKey = "invalid_key";
		assertEquals("val_12", map.get((short) 12));
		assertNull(map.get(invalidKey));
		assertTrue(map.containsKey((short) 12));
		assertFalse(map.containsKey(invalidKey));
		assertTrue(map.containsValue("val_12"));

		assertEquals("val_12", map.remove((short) 12));
		assertNull(map.remove(invalidKey));
	}

	@Test
	void cloneCreatesIndependentCopy() {
		ShortObjectHashMap<String> original = new ShortObjectHashMap<>();
		original.put((short) 1, "one");

		ShortObjectHashMap<String> clone = original.clone();
		assertNotSame(original, clone);
		assertEquals(1, clone.size());
		assertEquals("one", clone.get((short) 1));

		clone.put((short) 2, "two");
		assertEquals(2, clone.size());
		assertEquals(1, original.size());
	}
}
