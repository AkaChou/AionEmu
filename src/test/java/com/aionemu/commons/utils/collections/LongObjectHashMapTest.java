package com.aionemu.commons.utils.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

class LongObjectHashMapTest {

    @Test
    void storesAndReadsValuesWithoutBoxing() {
        LongObjectHashMap<String> map = new LongObjectHashMap<>();

        assertNull(map.put(210040000L, "heiron"));
        assertEquals("heiron", map.get(210040000L));
        assertEquals(1, map.size());
        assertNull(map.get(210040001L));
    }

    @Test
    void overwriteReturnsPreviousValue() {
        LongObjectHashMap<String> map = new LongObjectHashMap<>();
        map.put(7L, "first");

        assertEquals("first", map.put(7L, "second"));
        assertEquals("second", map.get(7L));
        assertEquals(1, map.size());
    }

    @Test
    void putIfAbsentKeepsExistingValue() {
        LongObjectHashMap<String> map = new LongObjectHashMap<>();

        assertNull(map.putIfAbsent(42L, "kept"));
        assertEquals("kept", map.putIfAbsent(42L, "ignored"));
        assertEquals("kept", map.get(42L));
        assertEquals(1, map.size());
    }

    @Test
    void acceptsIntKeysThroughWidening() {
        LongObjectHashMap<String> map = new LongObjectHashMap<>(4);
        int blockId = 123456;

        map.put(blockId, "block");

        assertEquals("block", map.get(blockId));
        assertEquals("block", map.get((long) blockId));
    }

    @Test
    void handlesLargeKeySpaceAndExtremeKeys() {
        LongObjectHashMap<Integer> map = new LongObjectHashMap<>();

        map.put(Long.MIN_VALUE, 1);
        map.put(Long.MAX_VALUE, 2);
        map.put(0L, 3);
        map.put(-1L, 4);

        assertEquals(1, map.get(Long.MIN_VALUE));
        assertEquals(2, map.get(Long.MAX_VALUE));
        assertEquals(3, map.get(0L));
        assertEquals(4, map.get(-1L));
        assertNull(map.get(1L));
    }

    @Test
    void growsPastLoadFactorAndKeepsAllEntries() {
        LongObjectHashMap<Object> map = new LongObjectHashMap<>(4);
        Object marker = new Object();

        for (long key = 0; key < 1_000; key++) {
            map.put(key, marker);
        }

        assertEquals(1_000, map.size());
        for (long key = 0; key < 1_000; key++) {
            assertSame(marker, map.get(key));
        }
    }

    @Test
    void clearKeepsCapacityForWorkspaceReuse() {
        LongObjectHashMap<String> map = new LongObjectHashMap<>(256);
        for (long key = 0; key < 500; key++) {
            map.put(key, "v");
        }

        map.clear();

        assertEquals(0, map.size());
        assertNull(map.get(0L));
        map.put(1_000_000L, "reused");
        assertEquals("reused", map.get(1_000_000L));
        assertEquals(1, map.size());
    }

    @Test
    void clearReleasesEverySlotAfterGrowth() {
        LongObjectHashMap<String> map = new LongObjectHashMap<>(4);
        for (long key = 0; key < 5_000; key++) {
            map.put(key, "v" + key);
        }

        map.clear();

        assertEquals(0, map.size());
        for (long key = 0; key < 5_000; key++) {
            assertNull(map.get(key));
        }
        // 复用同一实例重建：扩容后的槽位记账必须完整重置。
        // Reusing the same instance after growth: the slot bookkeeping must reset completely.
        for (long key = 0; key < 1_000; key++) {
            map.put(key, "w" + key);
        }
        assertEquals(1_000, map.size());
        for (long key = 0; key < 1_000; key++) {
            assertEquals("w" + key, map.get(key));
        }
        assertNull(map.get(1_000L));
    }

    @Test
    void clearThenEnsureCapacityKeepsTheTableUsable() {
        LongObjectHashMap<String> map = new LongObjectHashMap<>(4);
        map.put(1L, "a");

        map.clear();
        map.ensureCapacity(8_192);

        assertNull(map.get(1L));
        map.put(2L, "b");
        assertEquals("b", map.get(2L));
        map.clear();
        assertNull(map.get(2L));
        assertEquals(0, map.size());
    }

    @Test
    void collidingKeysRemainDistinct() {
        LongObjectHashMap<String> map = new LongObjectHashMap<>(8);
        // 这些键在掩码后会命中同一初始槽位，用来覆盖线性探测路径。
        // These keys land on the same initial slot, exercising linear probing.
        long mask = 7L;
        long first = 0L;
        long second = 8L;
        long third = 16L;

        map.put(first, "a");
        map.put(second, "b");
        map.put(third, "c");

        assertEquals("a", map.get(first));
        assertEquals("b", map.get(second));
        assertEquals("c", map.get(third));
        assertEquals("a", map.get(first & mask));
    }
}
