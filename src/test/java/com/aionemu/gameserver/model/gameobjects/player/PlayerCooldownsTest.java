package com.aionemu.gameserver.model.gameobjects.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.model.items.ItemCooldown;
import com.aionemu.gameserver.model.templates.item.ItemUseLimits;

/**
 * 固化 {@link PlayerCooldowns} 的容器语义：可空 map、live 视图、过期条目清除。
 * Pins down the {@link PlayerCooldowns} container semantics: nullable map, live view and
 * expired-entry eviction.
 */
class PlayerCooldownsTest {

	private final PlayerCooldowns cooldowns = new PlayerCooldowns();

	@Test
	void backingMapStaysNullUntilFirstWrite() {
		assertNull(cooldowns.getItemCoolDowns());
		assertEquals(0, cooldowns.getCoolDown(7));
		assertFalse(cooldowns.isUseDisabled(limits(7)));
		cooldowns.removeCoolDown(7);

		assertNull(cooldowns.getItemCoolDowns());
	}

	@Test
	void exposedMapIsTheLiveBackingMap() {
		cooldowns.addCoolDown(7, System.currentTimeMillis() + 60_000, 100);

		Map<Integer, ItemCooldown> backing = cooldowns.getItemCoolDowns();
		assertEquals(1, backing.size());
		assertSame(backing, cooldowns.getItemCoolDowns());
		assertEquals(100, backing.get(7).getUseDelay());
	}

	@Test
	void activeCooldownDisablesUseWhileExpiredEntriesAreEvicted() {
		cooldowns.addCoolDown(7, System.currentTimeMillis() + 60_000, 100);

		assertTrue(cooldowns.isUseDisabled(limits(7)));

		cooldowns.addCoolDown(8, System.currentTimeMillis() - 1, 100);

		// 已过期：判定为可用且条目被清除 / expired: usable again and the entry is evicted
		assertFalse(cooldowns.isUseDisabled(limits(8)));
		assertNull(cooldowns.getItemCoolDowns().get(8));
	}

	@Test
	void removalDropsTheEntryButKeepsTheMap() {
		cooldowns.addCoolDown(7, System.currentTimeMillis() + 60_000, 100);

		cooldowns.removeCoolDown(7);

		assertEquals(0, cooldowns.getCoolDown(7));
		assertTrue(cooldowns.getItemCoolDowns().isEmpty());
	}

	/**
	 * 构造指定延迟 ID 的使用限制。
	 * Builds use limits carrying the given delay id.
	 */
	private ItemUseLimits limits(int delayId) {
		ItemUseLimits limits = new ItemUseLimits();
		limits.setDelayId(delayId);
		return limits;
	}
}
