package com.aionemu.commons.utils.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * 可冻结 int 键只读表专项测试：冻结前后查询语义等价、二分命中、写防护与幂等冻结。
 * FrozenIntObjectTable tests: equivalent query semantics before/after freeze, binary-search hits,
 * write guards, and idempotent freezing.
 */
class FrozenIntObjectTableTest {

    @Test
    void buildModeQueriesMatchHashMapSemantics() {
        FrozenIntObjectTable<String> table = new FrozenIntObjectTable<>();
        assertNull(table.get(1));
        assertFalse(table.containsKey(1));
        assertNull(table.put(1, "a"));
        assertEquals("a", table.get(1));
        assertTrue(table.containsKey(1));
        assertEquals("a", table.putIfAbsent(1, "b"));
        assertEquals("a", table.get(1));
        assertEquals("a", table.put(1, "c"));
        assertEquals("c", table.computeIfAbsent(1, ignored -> "d"));
        assertEquals("c", table.remove(1));
        assertFalse(table.containsKey(1));
    }

    @Test
    void frozenQueriesAreEquivalentToBuildMode() {
        FrozenIntObjectTable<String> table = new FrozenIntObjectTable<>();
        int[] keys = { 210040001, 30001, 7, -5, 0, Integer.MAX_VALUE, Integer.MIN_VALUE, 210040000 };
        for (int i = 0; i < keys.length; i++) {
            table.put(keys[i], "v" + i);
        }

        List<String> before = new ArrayList<>();
        for (int key : keys) {
            before.add(table.get(key));
        }

        table.freeze();
        assertTrue(table.isFrozen());

        for (int i = 0; i < keys.length; i++) {
            assertSame(before.get(i), table.get(keys[i]), "frozen get must match build-mode get");
        }
        assertNull(table.get(42));
        assertFalse(table.containsKey(42));
        assertFalse(table.containsKey(Integer.MIN_VALUE + 1));
        assertTrue(table.containsKey(Integer.MAX_VALUE));
    }

    @Test
    void frozenTableRejectsWrites() {
        FrozenIntObjectTable<String> table = new FrozenIntObjectTable<>();
        table.put(1, "a");
        table.freeze();
        table.freeze();
        assertTrue(table.isFrozen());
        assertThrows(IllegalStateException.class, () -> table.put(2, "b"));
        assertThrows(IllegalStateException.class, () -> table.putIfAbsent(2, "b"));
        assertThrows(IllegalStateException.class, () -> table.computeIfAbsent(2, ignored -> "b"));
        assertThrows(IllegalStateException.class, () -> table.remove(1));
        assertEquals("a", table.get(1));
    }

    @Test
    void emptyTableFreezesAndQueriesMiss() {
        FrozenIntObjectTable<String> table = new FrozenIntObjectTable<>();
        table.freeze();
        assertNull(table.get(1));
        assertFalse(table.containsKey(1));
    }
}
