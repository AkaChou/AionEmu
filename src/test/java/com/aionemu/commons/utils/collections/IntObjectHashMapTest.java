package com.aionemu.commons.utils.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

class IntObjectHashMapTest {

	@Test
	void storesAndReadsValuesWithoutBoxing() {
		IntObjectHashMap<String> map = new IntObjectHashMap<>();

		assertNull(map.put(1001, "item_1001"));
		assertEquals("item_1001", map.get(1001));
		assertEquals(1, map.size());
		assertFalse(map.isEmpty());
		assertNull(map.get(1002));
		assertTrue(map.containsKey(1001));
		assertTrue(map.contains(1001));
		assertFalse(map.containsKey(1002));
	}

	@Test
	void overwriteReturnsPreviousValue() {
		IntObjectHashMap<String> map = new IntObjectHashMap<>();
		map.put(7, "first");

		assertEquals("first", map.put(7, "second"));
		assertEquals("second", map.get(7));
		assertEquals(1, map.size());
	}

	@Test
	void putIfAbsentKeepsExistingValue() {
		IntObjectHashMap<String> map = new IntObjectHashMap<>();

		assertNull(map.putIfAbsent(42, "kept"));
		assertEquals("kept", map.putIfAbsent(42, "ignored"));
		assertEquals("kept", map.get(42));
		assertEquals(1, map.size());
	}

	@Test
	void removesPrimitiveKeysCorrectly() {
		IntObjectHashMap<String> map = new IntObjectHashMap<>();
		map.put(10, "ten");
		map.put(20, "twenty");

		assertEquals("ten", map.remove(10));
		assertNull(map.get(10));
		assertFalse(map.containsKey(10));
		assertEquals(1, map.size());

		assertNull(map.remove(99));
		assertEquals(1, map.size());

		assertEquals("twenty", map.remove(20));
		assertTrue(map.isEmpty());
	}

	@Test
	void handlesCollisionsAndBackwardShiftDeletion() {
		IntObjectHashMap<String> map = new IntObjectHashMap<>(8);

		// 构造密集插入以产生线性探测碰撞链
		// Construct dense inserts to create a linear probing collision cluster
		for (int i = 0; i < 20; i++) {
			map.put(i, "val_" + i);
		}

		assertEquals(20, map.size());

		// 删除中间节点，触发反向移动填补（backward shift deletion）
		// Delete middle elements to trigger backward shift deletion
		assertEquals("val_5", map.remove(5));
		assertEquals("val_10", map.remove(10));
		assertEquals("val_15", map.remove(15));
		assertEquals(17, map.size());

		// 验证被删除项确实不存在，而未被删除项仍能全部正确命中
		// Verify deleted keys are gone while all remaining keys are still reachable
		assertNull(map.get(5));
		assertNull(map.get(10));
		assertNull(map.get(15));

		for (int i = 0; i < 20; i++) {
			if (i != 5 && i != 10 && i != 15) {
				assertEquals("val_" + i, map.get(i));
				assertTrue(map.containsKey(i));
			}
		}
	}

	@Test
	void handlesExtremeAndNegativeKeys() {
		IntObjectHashMap<Integer> map = new IntObjectHashMap<>();

		map.put(Integer.MIN_VALUE, (Integer) 1);
		map.put(Integer.MAX_VALUE, (Integer) 2);
		map.put(0, (Integer) 3);
		map.put(-1, (Integer) 4);
		map.put(-10086, (Integer) 5);

		assertEquals(1, map.get(Integer.MIN_VALUE));
		assertEquals(2, map.get(Integer.MAX_VALUE));
		assertEquals(3, map.get(0));
		assertEquals(4, map.get(-1));
		assertEquals(5, map.get(-10086));
		assertEquals(5, map.size());

		assertEquals(3, map.remove(0));
		assertNull(map.get(0));
		assertEquals(4, map.size());
	}

	@Test
	void growsPastLoadFactorAndKeepsAllEntries() {
		IntObjectHashMap<String> map = new IntObjectHashMap<>(8);
		int count = 5_000;

		for (int i = 0; i < count; i++) {
			map.put(i, "entity_" + i);
		}

		assertEquals(count, map.size());
		for (int i = 0; i < count; i++) {
			assertEquals("entity_" + i, map.get(i));
		}
	}

	@Test
	void keysArrayContainsAllKeys() {
		IntObjectHashMap<String> map = new IntObjectHashMap<>();
		map.put(100, "a");
		map.put(200, "b");
		map.put(300, "c");

		int[] keys = map.keys();
		assertEquals(3, keys.length);
		Arrays.sort(keys);
		assertEquals(100, keys[0]);
		assertEquals(200, keys[1]);
		assertEquals(300, keys[2]);
	}

	@Test
	void valuesCollectionIteration() {
		IntObjectHashMap<String> map = new IntObjectHashMap<>();
		map.put(1, "one");
		map.put(2, "two");
		map.put(3, "three");

		List<String> values = new ArrayList<>(map.values());

		assertEquals(3, values.size());
		assertTrue(values.contains("one"));
		assertTrue(values.contains("two"));
		assertTrue(values.contains("three"));
		assertTrue(map.containsValue("two"));
		assertFalse(map.containsValue("four"));
	}

	@Test
	void keySetAndEntrySetIteration() {
		IntObjectHashMap<String> map = new IntObjectHashMap<>();
		map.put(10, "A");
		map.put(20, "B");

		Set<Integer> keySet = map.keySet();
		assertEquals(2, keySet.size());
		assertTrue(keySet.contains(10));
		assertTrue(keySet.contains(20));

		Set<Map.Entry<Integer, String>> entries = map.entrySet();
		assertEquals(2, entries.size());
		Set<Integer> entryKeys = new HashSet<>();
		for (Map.Entry<Integer, String> entry : entries) {
			entryKeys.add(entry.getKey());
		}
		assertEquals(Set.of(10, 20), entryKeys);
	}

	@Test
	void computeIfAbsentPrimitiveAndBoxed() {
		IntObjectHashMap<String> map = new IntObjectHashMap<>();

		// 原生 int 重载
		String val1 = map.computeIfAbsent(55, k -> "computed_" + k);
		assertEquals("computed_55", val1);
		assertEquals("computed_55", map.get(55));

		String val2 = map.computeIfAbsent(55, _ -> "ignored");
		assertEquals("computed_55", val2);

		// 装箱重载
		String val3 = map.computeIfAbsent(Integer.valueOf(66), k -> "boxed_" + k);
		assertEquals("boxed_66", val3);
		assertEquals("boxed_66", map.get(66));
	}

	@Test
	void primitiveForEachIteratesAllEntries() {
		IntObjectHashMap<String> map = new IntObjectHashMap<>();
		map.put(1, "alpha");
		map.put(2, "beta");

		List<Integer> keys = new ArrayList<>();
		List<String> vals = new ArrayList<>();
		map.forEachEntry((k, v) -> {
			keys.add(k);
			vals.add(v);
		});

		assertEquals(2, keys.size());
		assertTrue(keys.contains(1));
		assertTrue(keys.contains(2));
		assertTrue(vals.contains("alpha"));
		assertTrue(vals.contains("beta"));
	}

	@Test
	void clearAndReuse() {
		IntObjectHashMap<String> map = new IntObjectHashMap<>(32);
		for (int i = 0; i < 100; i++) {
			map.put(i, "v" + i);
		}
		assertEquals(100, map.size());

		map.clear();
		assertEquals(0, map.size());
		assertTrue(map.isEmpty());
		assertNull(map.get(0));

		map.put(999, "new_val");
		assertEquals(1, map.size());
		assertEquals("new_val", map.get(999));
	}

	@Test
	void mapInterfaceCompatibility() {
		Map<Integer, String> map = new IntObjectHashMap<>();
		map.put(123, "val_123");

		Object nonIntKey = "not_an_int";
		Integer boxedKey = 123;
		assertEquals("val_123", map.get(123));
		assertEquals("val_123", map.get(boxedKey));
		assertNull(map.get(nonIntKey));
		assertTrue(map.containsKey(123));
		assertFalse(map.containsKey(nonIntKey));
		assertEquals("val_123", map.getOrDefault(123, "def"));
		assertEquals("def", map.getOrDefault(456, "def"));

		assertEquals("val_123", map.remove(123));
		assertNull(map.remove(nonIntKey));
	}

	@Test
	void cloneCreatesIndependentCopy() {
		IntObjectHashMap<String> original = new IntObjectHashMap<>();
		original.put(1, "one");
		original.put(2, "two");

		IntObjectHashMap<String> clone = original.clone();
		assertNotSame(original, clone);
		assertEquals(2, clone.size());
		assertEquals("one", clone.get(1));
		assertEquals("two", clone.get(2));

		clone.put(3, "three");
		assertEquals(3, clone.size());
		assertEquals(2, original.size());
		assertNull(original.get(3));
	}
}
