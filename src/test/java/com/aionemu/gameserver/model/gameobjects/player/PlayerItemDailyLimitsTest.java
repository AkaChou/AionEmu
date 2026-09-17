package com.aionemu.gameserver.model.gameobjects.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Iterator;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.model.templates.event.MaxCountOfDay;

/**
 * 固化 {@link PlayerItemDailyLimits} 的容器语义：惰性可空表、live 视图迭代删除与更新语义。
 * Pins down the {@link PlayerItemDailyLimits} container semantics: lazy nullable map, live-view
 * removal during iteration and update semantics.
 */
class PlayerItemDailyLimitsTest {

	private final PlayerItemDailyLimits limits = new PlayerItemDailyLimits();

	@Test
	void backingMapStaysNullUntilFirstWrite() {
		assertNull(limits.getItemMaxCounts());
		assertEquals(0, limits.getItemMaxCount(187100001));
		limits.removeItemMaxCount(187100001);
		limits.clear();

		assertNull(limits.getItemMaxCounts());
	}

	@Test
	void addThenUpdateKeepsSingleEntryPerItem() {
		limits.addItemMaxCount(187100001, 3);

		assertEquals(3, limits.getItemMaxCount(187100001));

		limits.addItemMaxCount(187100001, 5);

		assertEquals(5, limits.getItemMaxCount(187100001));
		assertEquals(1, limits.getItemMaxCounts().size());
	}

	@Test
	void exposedMapSupportsRemovalDuringIteration() {
		limits.addItemMaxCount(187100001, 1);
		limits.addItemMaxCount(187100002, 2);

		Map<Integer, MaxCountOfDay> backing = limits.getItemMaxCounts();
		assertNotNull(backing);
		assertSame(backing, limits.getItemMaxCounts());

		Iterator<Map.Entry<Integer, MaxCountOfDay>> iterator = backing.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<Integer, MaxCountOfDay> entry = iterator.next();
			iterator.remove();
		}

		// live 视图上迭代删除后，底层表已空但仍然存在 / live-view removal empties but keeps the map
		assertTrue(backing.isEmpty());
		assertEquals(0, limits.getItemMaxCount(187100001));
		assertNotNull(limits.getItemMaxCounts());
	}
}
